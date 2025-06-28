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
import org.eyespire.eyespireapi.model.enums.GenderType;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
                // Nếu đã có cuộc hẹn với paymentId này, trả về cuộc hẹn đó thay vì tạo mới
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
        
        // Tìm dịch vụ (nếu có)
        MedicalService service = null;
        if (appointmentDTO.getServiceId() != null) {
            service = medicalServiceRepository.findById(appointmentDTO.getServiceId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        }
        
        // Chuyển đổi ngày và giờ từ chuỗi sang LocalDateTime
        LocalDate appointmentDate = LocalDate.parse(appointmentDTO.getAppointmentDate());
        LocalTime appointmentTime = LocalTime.parse(appointmentDTO.getTimeSlot());
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);
        
        // Kiểm tra xem bác sĩ có khả dụng trong khung giờ này không (nếu đã chọn bác sĩ)
        if (doctor != null && !doctorService.isDoctorAvailable(doctor.getId(), appointmentDateTime)) {
            throw new RuntimeException("Bác sĩ không có sẵn trong khung giờ này");
        }
        
        // Tạo đối tượng lịch hẹn mới
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setAppointmentTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setNotes(appointmentDTO.getNotes());
        appointment.setPaymentId(appointmentDTO.getPaymentId());
        
        // Thiết lập thông tin bệnh nhân trực tiếp vào đối tượng Appointment
        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setPatientEmail(appointmentDTO.getPatientEmail());
        appointment.setPatientPhone(appointmentDTO.getPatientPhone());
        
        // Kiểm tra và đảm bảo thông tin bệnh nhân không null
        if (appointment.getPatientEmail() == null || appointment.getPatientEmail().isEmpty()) {
            throw new RuntimeException("Email bệnh nhân không được để trống");
        }
        if (appointment.getPatientName() == null || appointment.getPatientName().isEmpty()) {
            throw new RuntimeException("Tên bệnh nhân không được để trống");
        }
        if (appointment.getPatientPhone() == null || appointment.getPatientPhone().isEmpty()) {
            throw new RuntimeException("Số điện thoại bệnh nhân không được để trống");
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
            
            // Thiết lập vai trò là PATIENT
            patient.setRole(UserRole.PATIENT);
            
            patient = userRepository.save(patient);
        }
        
        appointment.setPatient(patient);
        
        // Lưu lịch hẹn
        appointment = appointmentRepository.save(appointment);
        
        // Tạo hóa đơn cho cuộc hẹn với tiền cọc
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
    
    // Các phương thức hiện có giữ nguyên...
    
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
    
    public Optional<Appointment> updateAppointment(Integer id, AppointmentDTO appointmentDTO) {
        return appointmentRepository.findById(id)
                .map(appointment -> {
                    // Cập nhật thông tin bác sĩ nếu có
                    if (appointmentDTO.getDoctorId() != null) {
                        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));
                        appointment.setDoctor(doctor);
                    }
                    
                    // Cập nhật thông tin dịch vụ nếu có
                    if (appointmentDTO.getServiceId() != null) {
                        MedicalService service = medicalServiceRepository.findById(appointmentDTO.getServiceId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
                        appointment.setService(service);
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
    
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
    
    /**
     * Lấy danh sách khung giờ trống theo ngày (không lọc theo bác sĩ)
     * Một khung giờ được coi là khả dụng nếu có ít nhất một bác sĩ làm việc trong khung giờ đó
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlotsByDate(LocalDate date) {
        // Tạo danh sách khung giờ mặc định
        List<DoctorTimeSlotDTO> timeSlots = createDefaultTimeSlots();
        
        // Lấy danh sách tất cả bác sĩ
        List<Doctor> allDoctors = doctorRepository.findAll();
        
        // Map để theo dõi số lượng bác sĩ có sẵn cho mỗi khung giờ
        Map<LocalTime, Integer> availableDoctorsCount = new HashMap<>();
        
        // Kiểm tra lịch làm việc của tất cả bác sĩ
        for (DoctorTimeSlotDTO slot : timeSlots) {
            int doctorsAvailableForSlot = 0;
            
            for (Doctor doctor : allDoctors) {
                // Kiểm tra xem bác sĩ có lịch làm việc trong ngày và khung giờ này không
                List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(
                        doctor.getId(), date);
                
                boolean isDoctorAvailableForSlot = false;
                for (DoctorAvailability availability : availabilities) {
                    // Kiểm tra xem khung giờ có nằm trong khoảng thời gian làm việc của bác sĩ không
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
            
            // Lưu số lượng bác sĩ có sẵn cho khung giờ này
            availableDoctorsCount.put(slot.getStartTime(), doctorsAvailableForSlot);
            
            // Nếu có ít nhất một bác sĩ khả dụng, đánh dấu là AVAILABLE
            if (doctorsAvailableForSlot > 0) {
                slot.setStatus(AvailabilityStatus.AVAILABLE);
                // Cập nhật số lượng slot có sẵn dựa trên số lượng bác sĩ
                slot.setAvailableCount(doctorsAvailableForSlot);
            }
        }
        
        // Lấy danh sách tất cả các lịch hẹn trong ngày
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        // Lấy tất cả các cuộc hẹn trong ngày (không bị hủy)
        List<Appointment> allAppointments = appointmentRepository.findByAppointmentTimeBetweenAndStatusNot(
                startOfDay, endOfDay, AppointmentStatus.CANCELED);
        
        // Lọc chỉ lấy các cuộc hẹn PENDING hoặc CONFIRMED, loại bỏ COMPLETED và các trạng thái khác
        List<Appointment> appointments = allAppointments.stream()
                .filter(appointment -> {
                    AppointmentStatus status = appointment.getStatus();
                    return status == AppointmentStatus.PENDING || status == AppointmentStatus.CONFIRMED;
                })
                .collect(Collectors.toList());
        
        // Đếm số lượng lịch hẹn cho mỗi khung giờ
        for (Appointment appointment : appointments) {
            LocalTime appointmentTime = appointment.getAppointmentTime().toLocalTime();
            
            for (DoctorTimeSlotDTO slot : timeSlots) {
                if (slot.getStartTime().equals(appointmentTime)) {
                    // Giảm số lượng slot có sẵn
                    slot.decreaseAvailableCount();
                    
                    // Nếu đã hết slot (không còn bác sĩ nào có sẵn), đánh dấu là BOOKED
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
        
        // Tạo các khung giờ từ 8h đến 16h, mỗi khung giờ cách nhau 1 tiếng
        for (int hour = 8; hour <= 16; hour++) {
            LocalTime startTime = LocalTime.of(hour, 0);
            LocalTime endTime = LocalTime.of(hour + 1, 0);
            
            // Mỗi khung giờ mặc định có thể phục vụ nhiều bệnh nhân cùng lúc
            DoctorTimeSlotDTO slot = new DoctorTimeSlotDTO(startTime, endTime, AvailabilityStatus.AVAILABLE);
            timeSlots.add(slot);
        }
        
        return timeSlots;
    }
    
    /**
     * Chuyển trạng thái cuộc hẹn sang chờ thanh toán sau khi bác sĩ tạo hồ sơ bệnh án
     */
    @Transactional
    public Optional<Appointment> setAppointmentWaitingPayment(Integer appointmentId, BigDecimal totalAmount) {
        return appointmentRepository.findById(appointmentId)
                .map(appointment -> {
                    // Cập nhật hóa đơn
                    appointmentInvoiceService.updateInvoiceAfterMedicalRecord(appointmentId, totalAmount);
                    
                    // Trả về cuộc hẹn đã cập nhật
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
                    // Cập nhật hóa đơn và trạng thái cuộc hẹn
                    appointmentInvoiceService.markAsPaid(appointmentId, transactionId);
                    
                    // Trả về cuộc hẹn đã cập nhật
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
}
