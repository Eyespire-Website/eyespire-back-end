package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.model.Payment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentType;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.AppointmentInvoiceRepository;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorAvailabilityRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.MedicalServiceRepository;
import org.eyespire.eyespireapi.repository.PaymentRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private MedicalServiceRepository medicalServiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private AppointmentInvoiceRepository appointmentInvoiceRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AppointmentInvoiceService appointmentInvoiceService;

    // Số tiền cọc mặc định
    private static final BigDecimal DEFAULT_DEPOSIT_AMOUNT = new BigDecimal("10000");

    /**
     * Tạo lịch hẹn mới
     */
    @Transactional
    public Appointment createAppointment(AppointmentDTO appointmentDTO) {
        // Kiểm tra xem đã có cuộc hẹn nào được tạo với paymentId này chưa
        if (appointmentDTO.getPaymentId() != null) {
            List<Appointment> existingAppointments = appointmentRepository.findByPaymentId(appointmentDTO.getPaymentId());
            if (!existingAppointments.isEmpty()) {
                System.out.println("Đã tìm thấy cuộc hẹn hiện có với paymentId: " + appointmentDTO.getPaymentId());
                return existingAppointments.get(0);
            }
        }

        // Tìm bác sĩ (nếu có)
        Doctor doctor = null;
        if (appointmentDTO.getDoctorId() != null) {
            doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));
        }

        // Tìm danh sách dịch vụ (nếu có)
        List<MedicalService> services = new ArrayList<>();
        if (appointmentDTO.getServiceIds() != null && !appointmentDTO.getServiceIds().isEmpty()) {
            services = medicalServiceRepository.findAllById(appointmentDTO.getServiceIds());
            if (services.size() != appointmentDTO.getServiceIds().size()) {
                throw new IllegalArgumentException("Một hoặc nhiều dịch vụ không hợp lệ");
            }
        } else if (appointmentDTO.getServiceId() != null) {
            // Hỗ trợ tương thích với serviceId cũ
            MedicalService service = medicalServiceRepository.findById(appointmentDTO.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
            services.add(service);
        }

        // Chuyển đổi ngày và giờ từ chuỗi sang LocalDateTime
        LocalDate appointmentDate = LocalDate.parse(appointmentDTO.getAppointmentDate());
        LocalTime appointmentTime = LocalTime.parse(appointmentDTO.getTimeSlot());
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);

        // Kiểm tra xem bác sĩ có khả dụng trong khung giờ này không (nếu đã chọn bác sĩ)
        if (doctor != null && !doctorService.isDoctorAvailable(doctor.getId(), appointmentDateTime)) {
            throw new RuntimeException("Bác sĩ không có sẵn trong khung giờ này");
        }

        // Tìm bệnh nhân
        User patient = null;
        if (appointmentDTO.getUserId() != null) {
            patient = userRepository.findById(appointmentDTO.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bệnh nhân"));
        } else {
            // Tạo bệnh nhân mới nếu không có ID
            patient = new User();
            patient.setName(appointmentDTO.getPatientName());
            patient.setEmail(appointmentDTO.getPatientEmail());
            patient.setPhone(appointmentDTO.getPatientPhone());
            patient.setRole(UserRole.PATIENT);
            patient = userRepository.save(patient);
        }

        // Kiểm tra và đảm bảo thông tin bệnh nhân không null
        if (appointmentDTO.getPatientEmail() == null || appointmentDTO.getPatientEmail().isEmpty()) {
            throw new RuntimeException("Email bệnh nhân không được để trống");
        }
        if (appointmentDTO.getPatientName() == null || appointmentDTO.getPatientName().isEmpty()) {
            throw new RuntimeException("Tên bệnh nhân không được để trống");
        }
        if (appointmentDTO.getPatientPhone() == null || appointmentDTO.getPatientPhone().isEmpty()) {
            throw new RuntimeException("Số điện thoại bệnh nhân không được để trống");
        }

        // Tạo đối tượng lịch hẹn mới
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setServices(services);
        appointment.setAppointmentTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(appointmentDTO.getNotes());
        appointment.setPaymentId(appointmentDTO.getPaymentId());
        appointment.setPatient(patient);
        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setPatientEmail(appointmentDTO.getPatientEmail());
        appointment.setPatientPhone(appointmentDTO.getPatientPhone());

        // Lưu lịch hẹn
        appointment = appointmentRepository.save(appointment);

        // Tạo hóa đơn cho cuộc hẹn với tiền cọc
        BigDecimal totalAmount = services.stream()
                .map(MedicalService::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        AppointmentInvoice invoice = appointmentInvoiceService.createInvoice(appointment, DEFAULT_DEPOSIT_AMOUNT);

        // Tạo thanh toán tiền cọc
        Payment depositPayment = new Payment();
        depositPayment.setTransactionNo(appointmentDTO.getPaymentId() != null ? appointmentDTO.getPaymentId().toString() : "DEPOSIT-" + appointment.getId());
        depositPayment.setAmount(DEFAULT_DEPOSIT_AMOUNT);
        depositPayment.setStatus(PaymentStatus.COMPLETED);
        depositPayment.setAppointmentInvoice(invoice);
        depositPayment.setPaymentType(PaymentType.DEPOSIT);
        depositPayment.setPaymentDate(LocalDateTime.now());
        depositPayment.setUserId(patient.getId());
        paymentRepository.save(depositPayment);

        return appointment;
    }

    /**
     * Lấy danh sách lịch hẹn của bệnh nhân
     */
    public List<Appointment> getAppointmentsByPatient(Integer patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentTimeDesc(patientId);
    }

    /**
     * Lấy danh sách lịch hẹn của bác sĩ
     */
    public List<Appointment> getAppointmentsByDoctor(Integer doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentTimeDesc(doctorId);
    }

    /**
     * Lấy thông tin chi tiết lịch hẹn
     */
    public Optional<Appointment> getAppointmentById(Integer id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Cập nhật trạng thái lịch hẹn
     */
    public Optional<Appointment> updateAppointmentStatus(Integer id, AppointmentStatus status) {
        return appointmentRepository.findById(id)
                .map(appointment -> {
                    appointment.setStatus(status);
                    return appointmentRepository.save(appointment);
                });
    }

    /**
     * Cập nhật danh sách dịch vụ của lịch hẹn
     */
    @Transactional
    public Optional<Appointment> updateAppointmentServices(Integer appointmentId, List<Integer> serviceIds) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (!appointmentOpt.isPresent()) {
            throw new IllegalArgumentException("Cuộc hẹn không tồn tại!");
        }
        List<MedicalService> services = medicalServiceRepository.findAllById(serviceIds);
        if (services.size() != serviceIds.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều dịch vụ không hợp lệ");
        }
        Appointment appointment = appointmentOpt.get();
        appointment.setServices(services);
        return Optional.of(appointmentRepository.save(appointment));
    }

    /**
     * Hủy lịch hẹn
     */
    public Optional<Appointment> cancelAppointment(Integer id) {
        return updateAppointmentStatus(id, AppointmentStatus.CANCELED);
    }

    /**
     * Lấy danh sách lịch hẹn theo ngày
     */
    public List<Appointment> getAppointmentsByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return appointmentRepository.findByAppointmentTimeBetweenOrderByAppointmentTimeAsc(startOfDay, endOfDay);
    }

    /**
     * Lấy danh sách lịch hẹn của bác sĩ theo ngày
     */
    public List<Appointment> getAppointmentsByDoctorAndDate(Integer doctorId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(doctorId, startOfDay, endOfDay);
    }

    /**
     * Cập nhật thông tin lịch hẹn
     */
    @Transactional
    public Optional<Appointment> updateAppointment(Integer id, AppointmentDTO appointmentDTO) {
        return appointmentRepository.findById(id)
                .map(appointment -> {
                    // Cập nhật thông tin bác sĩ nếu có
                    if (appointmentDTO.getDoctorId() != null) {
                        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));
                        appointment.setDoctor(doctor);
                    }

                    // Cập nhật danh sách dịch vụ nếu có
                    if (appointmentDTO.getServiceIds() != null && !appointmentDTO.getServiceIds().isEmpty()) {
                        List<MedicalService> services = medicalServiceRepository.findAllById(appointmentDTO.getServiceIds());
                        if (services.size() != appointmentDTO.getServiceIds().size()) {
                            throw new IllegalArgumentException("Một hoặc nhiều dịch vụ không hợp lệ");
                        }
                        appointment.setServices(services);
                    } else if (appointmentDTO.getServiceId() != null) {
                        // Hỗ trợ tương thích với serviceId cũ
                        MedicalService service = medicalServiceRepository.findById(appointmentDTO.getServiceId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
                        appointment.setServices(List.of(service));
                    }

                    // Cập nhật thời gian nếu có
                    if (appointmentDTO.getAppointmentDate() != null && appointmentDTO.getTimeSlot() != null) {
                        LocalDate appointmentDate = LocalDate.parse(appointmentDTO.getAppointmentDate());
                        LocalTime appointmentTime = LocalTime.parse(appointmentDTO.getTimeSlot());
                        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);

                        // Kiểm tra xem bác sĩ có khả dụng trong khung giờ này không
                        if (appointment.getDoctor() != null && !doctorService.isDoctorAvailable(appointment.getDoctor().getId(), appointmentDateTime)) {
                            throw new RuntimeException("Bác sĩ không có sẵn trong khung giờ này");
                        }
                        appointment.setAppointmentTime(appointmentDateTime);
                    }

                    // Cập nhật thông tin bệnh nhân
                    if (appointmentDTO.getPatientName() != null) {
                        appointment.setPatientName(appointmentDTO.getPatientName());
                    }
                    if (appointmentDTO.getPatientEmail() != null) {
                        appointment.setPatientEmail(appointmentDTO.getPatientEmail());
                    }
                    if (appointmentDTO.getPatientPhone() != null) {
                        appointment.setPatientPhone(appointmentDTO.getPatientPhone());
                    }

                    // Cập nhật ghi chú nếu có
                    if (appointmentDTO.getNotes() != null) {
                        appointment.setNotes(appointmentDTO.getNotes());
                    }

                    // Cập nhật trạng thái nếu có
                    if (appointmentDTO.getStatus() != null) {
                        appointment.setStatus(AppointmentStatus.valueOf(appointmentDTO.getStatus()));
                    }

                    return appointmentRepository.save(appointment);
                });
    }

    /**
     * Lấy danh sách tất cả lịch hẹn
     */
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Lấy danh sách khung giờ trống theo ngày
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlotsByDate(LocalDate date) {
        List<DoctorTimeSlotDTO> timeSlots = createDefaultTimeSlots();
        List<Doctor> allDoctors = doctorRepository.findAll();
        Map<LocalTime, Integer> availableDoctorsCount = new HashMap<>();

        for (DoctorTimeSlotDTO slot : timeSlots) {
            int doctorsAvailableForSlot = 0;
            for (Doctor doctor : allDoctors) {
                List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(
                        doctor.getId(), date);
                boolean isDoctorAvailableForSlot = false;
                for (DoctorAvailability availability : availabilities) {
                    if (!slot.getStartTime().isBefore(availability.getStartTime()) &&
                            !slot.getEndTime().isAfter(availability.getEndTime())) {
                        isDoctorAvailableForSlot = true;
                        break;
                    }
                }
                if (isDoctorAvailableForSlot) {
                    doctorsAvailableForSlot++;
                }
            }
            availableDoctorsCount.put(slot.getStartTime(), doctorsAvailableForSlot);
            if (doctorsAvailableForSlot > 0) {
                slot.setStatus(AvailabilityStatus.AVAILABLE);
                slot.setAvailableCount(doctorsAvailableForSlot);
            }
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        List<Appointment> appointments = appointmentRepository.findByAppointmentTimeBetweenAndStatusNot(
                        startOfDay, endOfDay, AppointmentStatus.CANCELED)
                .stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.PENDING || appointment.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.toList());

        for (Appointment appointment : appointments) {
            LocalTime appointmentTime = appointment.getAppointmentTime().toLocalTime();
            for (DoctorTimeSlotDTO slot : timeSlots) {
                if (slot.getStartTime().equals(appointmentTime)) {
                    slot.decreaseAvailableCount();
                    if (slot.getAvailableCount() <= 0) {
                        slot.setStatus(AvailabilityStatus.BOOKED);
                    }
                    break;
                }
            }
        }

        return timeSlots;
    }

    /**
     * Tạo danh sách khung giờ mặc định từ 8h đến 16h
     */
    private List<DoctorTimeSlotDTO> createDefaultTimeSlots() {
        List<DoctorTimeSlotDTO> timeSlots = new ArrayList<>();
        for (int hour = 8; hour <= 16; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);
            DoctorTimeSlotDTO slot = new DoctorTimeSlotDTO(startTime, endTime, AvailabilityStatus.AVAILABLE);
            timeSlots.add(slot);
        }
        return timeSlots;
    }

    /**
     * Chuyển trạng thái cuộc hẹn sang chờ thanh toán
     */
    @Transactional
    public Optional<Appointment> setAppointmentWaitingPayment(Integer appointmentId, BigDecimal totalAmount) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.WAITING_PAYMENT);
                    appointmentInvoiceService.updateInvoiceAfterMedicalRecord(appointmentId, totalAmount);
                    return appointmentRepository.save(appointment);
                });
    }

    /**
     * Đánh dấu cuộc hẹn đã thanh toán và chuyển trạng thái sang hoàn thành
     */
    @Transactional
    public Optional<Appointment> markAppointmentAsPaid(Integer appointmentId, String transactionId) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> {
                    appointment.setStatus(AppointmentStatus.COMPLETED);
                    appointmentInvoiceService.markAsPaid(appointmentId, transactionId);
                    return appointmentRepository.save(appointment);
                });
    }

    /**
     * Lấy danh sách cuộc hẹn đang chờ thanh toán
     */
    public List<Appointment> getWaitingPaymentAppointments() {
        return appointmentRepository.findByStatusOrderByAppointmentTimeDesc(AppointmentStatus.WAITING_PAYMENT);
    }

    /**
     * Lấy danh sách cuộc hẹn đang chờ thanh toán của bác sĩ
     */
    public List<Appointment> getWaitingPaymentAppointmentsByDoctor(Integer doctorId) {
        return appointmentRepository.findByDoctorIdAndStatusOrderByAppointmentTimeDesc(doctorId, AppointmentStatus.WAITING_PAYMENT);
    }

    /**
     * Lấy danh sách tất cả dịch vụ y tế
     */
    public List<MedicalService> getMedicalServices() {
        return medicalServiceRepository.findAll();
    }
}