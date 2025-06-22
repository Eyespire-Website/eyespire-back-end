package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.model.enums.GenderType;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorAvailabilityRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.eyespire.eyespireapi.repository.MedicalServiceRepository;
import org.eyespire.eyespireapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    /**
     * Lấy danh sách khung giờ trống theo ngày (không lọc theo bác sĩ)
     * Một khung giờ được coi là khả dụng nếu có ít nhất một bác sĩ làm việc trong khung giờ đó
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlotsByDate(LocalDate date) {
        // Tạo danh sách khung giờ mặc định từ 8h đến 16h
        List<DoctorTimeSlotDTO> timeSlots = createDefaultTimeSlots();
        
        // Mặc định tất cả các khung giờ là UNAVAILABLE
        for (DoctorTimeSlotDTO slot : timeSlots) {
            slot.setStatus(AvailabilityStatus.UNAVAILABLE);
        }
        
        // Lấy tất cả các bác sĩ
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
        
        // Lấy danh sách tất cả các lịch hẹn trong ngày (không bị hủy)
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        
        List<Appointment> appointments = appointmentRepository.findByAppointmentTimeBetweenAndStatusNot(
                startOfDay, endOfDay, AppointmentStatus.CANCELED);
        
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
}
