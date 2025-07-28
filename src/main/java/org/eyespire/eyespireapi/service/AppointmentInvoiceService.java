package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.InvoiceCreationRequestDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentType;
import org.eyespire.eyespireapi.model.enums.PrescriptionStatus;
import org.eyespire.eyespireapi.repository.AppointmentInvoiceRepository;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.MedicalServiceRepository;
import org.eyespire.eyespireapi.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppointmentInvoiceService {

    @Autowired
    private AppointmentInvoiceRepository appointmentInvoiceRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;
    /**
     * Tạo hóa đơn mới cho cuộc hẹn
     */
    @Transactional
    public AppointmentInvoice createInvoice(Appointment appointment, BigDecimal depositAmount) {
        AppointmentInvoice invoice = new AppointmentInvoice();
        invoice.setAppointment(appointment);
        invoice.setTotalAmount(depositAmount);
        invoice.setDepositAmount(depositAmount);
        invoice.setRemainingAmount(BigDecimal.ZERO);
        invoice.setIsFullyPaid(true);
        invoice.setCreatedAt(LocalDateTime.now());
        invoice.setPrescriptionStatus(PrescriptionStatus.NOT_BUY); // NEW: Default PrescriptionStatus

        return appointmentInvoiceRepository.save(invoice);
    }

    @Transactional
    public AppointmentInvoice updateOrCreateInvoice(Integer appointmentId, List<Integer> serviceIds, List<InvoiceCreationRequestDTO.MedicationDTO> medications) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy lịch hẹn với ID: " + appointmentId);
        }

        Appointment appointment = appointmentOpt.get();
        Optional<AppointmentInvoice> invoiceOpt = appointmentInvoiceRepository.findByAppointmentId(appointmentId);
        AppointmentInvoice invoice = invoiceOpt.orElseGet(() -> {
            AppointmentInvoice newInvoice = new AppointmentInvoice();
            newInvoice.setAppointment(appointment);
            newInvoice.setCreatedAt(LocalDateTime.now());
            newInvoice.setDepositAmount(BigDecimal.ZERO);
            newInvoice.setIsFullyPaid(false);
            return newInvoice;
        });

        // Cập nhật danh sách dịch vụ
        BigDecimal totalAmount = BigDecimal.ZERO;
        if (serviceIds != null && !serviceIds.isEmpty()) {
            List<MedicalService> services = medicalServiceRepository.findAllById(serviceIds);
            if (services.size() != serviceIds.size()) {
                throw new IllegalArgumentException("Một hoặc nhiều ID dịch vụ không hợp lệ");
            }
            totalAmount = services.stream()
                    .map(MedicalService::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {

        }

        // Xử lý đơn thuốc
        if (medications != null && !medications.isEmpty()) {
            BigDecimal medicationTotal = medications.stream()
                    .map(med -> med.getPrice().multiply(new BigDecimal(med.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAmount = totalAmount.add(medicationTotal);
            invoice.setPrescriptionStatus(PrescriptionStatus.PENDING);
        } else {
            invoice.setPrescriptionStatus(PrescriptionStatus.NOT_BUY);
        }

        // Cập nhật tổng chi phí và số tiền còn lại
        invoice.setTotalAmount(totalAmount);
        invoice.setRemainingAmount(totalAmount.subtract(invoice.getDepositAmount() != null ? invoice.getDepositAmount() : BigDecimal.ZERO));
        return appointmentInvoiceRepository.save(invoice);
    }

    @Transactional
    public AppointmentInvoice createInvoice(Integer appointmentId, List<Integer> serviceIds, List<InvoiceCreationRequestDTO.MedicationDTO> medications) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Không tìm thấy lịch hẹn với ID: " + appointmentId);
        }

        Appointment appointment = appointmentOpt.get();
        AppointmentInvoice invoice = new AppointmentInvoice();
        invoice.setAppointment(appointment);
        invoice.setPrescriptionStatus(PrescriptionStatus.NOT_BUY);
        invoice.setCreatedAt(LocalDateTime.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        if (serviceIds != null && !serviceIds.isEmpty()) {
            List<MedicalService> services = medicalServiceRepository.findAllById(serviceIds);
            totalAmount = services.stream()
                    .map(MedicalService::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        if (medications != null && !medications.isEmpty()) {
            BigDecimal medicationTotal = medications.stream()
                    .map(med -> med.getPrice().multiply(new BigDecimal(med.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalAmount = totalAmount.add(medicationTotal);
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setRemainingAmount(totalAmount);
        invoice.setDepositAmount(BigDecimal.ZERO);
        invoice.setIsFullyPaid(false);
        return appointmentInvoiceRepository.save(invoice);
    }

    /**
     * Cập nhật hóa đơn sau khi bác sĩ tạo hồ sơ bệnh án
     */
    @Transactional
    public AppointmentInvoice updateInvoiceAfterMedicalRecord(Integer appointmentId, BigDecimal totalAmount) {
        Optional<AppointmentInvoice> optionalInvoice = appointmentInvoiceRepository.findByAppointmentId(appointmentId);
        
        if (optionalInvoice.isEmpty()) {
            throw new RuntimeException("Không tìm thấy hóa đơn cho cuộc hẹn này");
        }
        
        AppointmentInvoice invoice = optionalInvoice.get();
        BigDecimal depositAmount = invoice.getDepositAmount();
        
        // Cập nhật tổng chi phí và số tiền còn lại
        invoice.setTotalAmount(totalAmount);
        invoice.setRemainingAmount(totalAmount.subtract(depositAmount));
        invoice.setIsFullyPaid(false); // Đánh dấu là chưa thanh toán đầy đủ
        
        // Cập nhật trạng thái cuộc hẹn sang WAITING_PAYMENT
        // NHƯƠNG: Giữ nguyên trạng thái COMPLETED nếu đã hoàn tất để tránh ẩn hồ sơ bệnh án
        Appointment appointment = invoice.getAppointment();
        System.out.println("[INVOICE UPDATE DEBUG] Current appointment status: " + appointment.getStatus());
        
        // Chỉ thay đổi status nếu chưa COMPLETED
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            appointment.setStatus(AppointmentStatus.WAITING_PAYMENT);
            System.out.println("[INVOICE UPDATE DEBUG] Changed status to WAITING_PAYMENT");
        } else {
            System.out.println("[INVOICE UPDATE DEBUG] Preserving COMPLETED status to keep medical records visible");
        }
        appointmentRepository.save(appointment);
        
        return appointmentInvoiceRepository.save(invoice);
    }
    
    /**
     * Đánh dấu hóa đơn đã thanh toán đầy đủ
     */
    @Transactional
    public AppointmentInvoice markAsPaid(Integer appointmentId, String transactionId) {
        Optional<AppointmentInvoice> optionalInvoice = appointmentInvoiceRepository.findByAppointmentId(appointmentId);
        
        if (optionalInvoice.isEmpty()) {
            throw new RuntimeException("Không tìm thấy hóa đơn cho cuộc hẹn này");
        }
        
        AppointmentInvoice invoice = optionalInvoice.get();
        
        // Tạo thanh toán cuối cùng
        Payment finalPayment = new Payment();
        finalPayment.setTransactionNo(transactionId);
        finalPayment.setAmount(invoice.getRemainingAmount());
        finalPayment.setStatus(PaymentStatus.COMPLETED);
        finalPayment.setAppointmentInvoice(invoice);
        finalPayment.setPaymentType(PaymentType.FINAL);
        finalPayment.setPaymentDate(LocalDateTime.now());
        finalPayment.setUserId(invoice.getAppointment().getPatient().getId());
        paymentRepository.save(finalPayment);
        
        // Cập nhật hóa đơn
        invoice.setTransactionId(transactionId);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setIsFullyPaid(true);
        
        // Cập nhật trạng thái cuộc hẹn sang COMPLETED
        Appointment appointment = invoice.getAppointment();
        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointmentRepository.save(appointment);
        
        return appointmentInvoiceRepository.save(invoice);
    }
    
    /**
     * Lấy hóa đơn theo ID cuộc hẹn
     */
    public Optional<AppointmentInvoice> getInvoiceByAppointmentId(Integer appointmentId) {
        return appointmentInvoiceRepository.findByAppointmentId(appointmentId);
    }
    
    /**
     * Lấy danh sách hóa đơn chưa thanh toán đầy đủ
     */
    public List<AppointmentInvoice> getUnpaidInvoices() {
        return appointmentInvoiceRepository.findByIsFullyPaidFalse();
    }

    @Transactional
    public Optional<AppointmentInvoice> updatePrescriptionStatus(Integer appointmentId, PrescriptionStatus status) {
        Optional<AppointmentInvoice> invoiceOpt = appointmentInvoiceRepository.findByAppointmentId(appointmentId);
        if (!invoiceOpt.isPresent()) {
            return Optional.empty();
        }

        AppointmentInvoice invoice = invoiceOpt.get();
        invoice.setPrescriptionStatus(status);

        if (status == PrescriptionStatus.NOT_BUY) {
            List<MedicalService> services = invoice.getAppointment().getServices();
            BigDecimal serviceTotal = services.stream()
                    .map(MedicalService::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setTotalAmount(serviceTotal);
            invoice.setRemainingAmount(serviceTotal.subtract(invoice.getDepositAmount() != null ? invoice.getDepositAmount() : BigDecimal.ZERO));
        } else if (status == PrescriptionStatus.PENDING) {
            // Medication costs should already be included in totalAmount from invoice creation
        }

        return Optional.of(appointmentInvoiceRepository.save(invoice));
    }
    
    /**
     * Lấy danh sách hóa đơn đã thanh toán đầy đủ
     */
    public List<AppointmentInvoice> getPaidInvoices() {
        return appointmentInvoiceRepository.findByIsFullyPaidTrue();
    }
}