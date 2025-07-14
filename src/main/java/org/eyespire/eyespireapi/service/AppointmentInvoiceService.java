package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentType;
import org.eyespire.eyespireapi.repository.AppointmentInvoiceRepository;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
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

    /**
     * Tạo hóa đơn mới cho cuộc hẹn
     */
    @Transactional
    public AppointmentInvoice createInvoice(Appointment appointment, BigDecimal depositAmount) {
        AppointmentInvoice invoice = new AppointmentInvoice();
        invoice.setAppointment(appointment);
        invoice.setTotalAmount(depositAmount); // Ban đầu, tổng chi phí chỉ là tiền cọc
        invoice.setDepositAmount(depositAmount);
        invoice.setRemainingAmount(BigDecimal.ZERO); // Ban đầu, số tiền còn lại là 0
        invoice.setIsFullyPaid(true); // Ban đầu, đã thanh toán đầy đủ (chỉ có tiền cọc)
        invoice.setCreatedAt(LocalDateTime.now());
        
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
    
    /**
     * Lấy danh sách hóa đơn đã thanh toán đầy đủ
     */
    public List<AppointmentInvoice> getPaidInvoices() {
        return appointmentInvoiceRepository.findByIsFullyPaidTrue();
    }
}
