package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.GenderType;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.MedicalServiceRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

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
    
    /**
     * Tạo lịch hẹn mới
     */
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
        
        // Tìm bác sĩ
        Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bác sĩ"));
        
        // Tìm dịch vụ
        MedicalService service = medicalServiceRepository.findById(appointmentDTO.getServiceId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dịch vụ"));
        
        // Chuyển đổi ngày và giờ từ chuỗi sang LocalDateTime
        LocalDate appointmentDate = LocalDate.parse(appointmentDTO.getAppointmentDate());
        LocalTime appointmentTime = LocalTime.parse(appointmentDTO.getTimeSlot());
        LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);
        
        // Kiểm tra xem bác sĩ có khả dụng trong khung giờ này không
        if (!doctorService.isDoctorAvailable(doctor.getId(), appointmentDateTime)) {
            throw new RuntimeException("Bác sĩ không có sẵn trong khung giờ này");
        }
        
        // Tạo đối tượng lịch hẹn mới
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setService(service);
        appointment.setAppointmentTime(appointmentDateTime);
        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setPatientEmail(appointmentDTO.getPatientEmail());
        appointment.setPatientPhone(appointmentDTO.getPatientPhone());
        appointment.setNotes(appointmentDTO.getNotes());
        appointment.setStatus(AppointmentStatus.PENDING);
        
        // Liên kết với người dùng nếu có
        if (appointmentDTO.getUserId() != null) {
            Optional<User> user = userRepository.findById(appointmentDTO.getUserId());
            user.ifPresent(appointment::setPatient);
        }
        
        // Lưu paymentId nếu có
        if (appointmentDTO.getPaymentId() != null) {
            appointment.setPaymentId(appointmentDTO.getPaymentId());
        }
        
        // Lưu lịch hẹn vào database
        return appointmentRepository.save(appointment);
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
    public Appointment updateAppointmentStatus(Integer id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lịch hẹn"));
        
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }
    
    /**
     * Hủy lịch hẹn
     */
    public Appointment cancelAppointment(Integer id) {
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
        
        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(
                doctorId, startOfDay, endOfDay);
    }
    public Appointment updateAppointment(Integer id, AppointmentDTO appointmentDTO) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch hẹn"));

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể cập nhật lịch hẹn ở trạng thái PENDING");
        }

        // Cập nhật các trường từ DTO
        appointment.setService(medicalServiceRepository.findById(appointmentDTO.getServiceId())
                .orElseThrow(() -> new IllegalArgumentException("Dịch vụ không tồn tại")));
        appointment.setDoctor(doctorRepository.findById(appointmentDTO.getDoctorId())
                .orElseThrow(() -> new IllegalArgumentException("Bác sĩ không tồn tại")));
        appointment.setAppointmentTime(LocalDateTime.parse(
                appointmentDTO.getAppointmentDate() + "T" + appointmentDTO.getTimeSlot()));
        appointment.setStatus(AppointmentStatus.valueOf(appointmentDTO.getStatus()));
        appointment.setNotes(appointmentDTO.getNotes());
        appointment.setPatientName(appointmentDTO.getPatientName());
        appointment.setPatientEmail(appointmentDTO.getPatientEmail());
        appointment.setPatientPhone(appointmentDTO.getPatientPhone());

        // Cập nhật thông tin bệnh nhân nếu có
        if (appointmentDTO.getPatient() != null) {
            User patient = userRepository.findById(appointmentDTO.getPatient().getId())
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setId(appointmentDTO.getPatient().getId());
                        return newUser;
                    });
            patient.setName(appointmentDTO.getPatient().getName());
            patient.setEmail(appointmentDTO.getPatient().getEmail());
            patient.setPhone(appointmentDTO.getPatient().getPhone());
            patient.setProvince(appointmentDTO.getPatient().getProvince());
            patient.setDistrict(appointmentDTO.getPatient().getDistrict());
            patient.setWard(appointmentDTO.getPatient().getWard());
            patient.setAddressDetail(appointmentDTO.getPatient().getAddressDetail());
            patient.setAddressDetail(appointmentDTO.getPatient().getVillage());
            if (appointmentDTO.getPatient().getGender() != null) {
                patient.setGender(GenderType.valueOf(appointmentDTO.getPatient().getGender().toUpperCase()));
            }
            if (appointmentDTO.getPatient().getDateOfBirth() != null) {
                patient.setDateOfBirth(LocalDate.parse(appointmentDTO.getPatient().getDateOfBirth()));
            }
            userRepository.save(patient);
            appointment.setPatient(patient);
        }

        return appointmentRepository.save(appointment);
    }

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}
