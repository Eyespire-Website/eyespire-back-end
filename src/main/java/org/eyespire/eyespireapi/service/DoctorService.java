package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.DoctorAvailabilityRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DoctorService {
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    /**
     * Lấy danh sách tất cả bác sĩ
     */
    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }
    
    /**
     * Lấy thông tin bác sĩ theo ID
     */
    public Optional<Doctor> getDoctorById(Integer id) {
        return doctorRepository.findById(id);
    }
    
    /**
     * Lấy danh sách bác sĩ theo chuyên khoa
     */
    public List<Doctor> getDoctorsBySpecialty(Integer specialtyId) {
        return doctorRepository.findBySpecialtyId(specialtyId);
    }
    
    /**
     * Lấy danh sách khung giờ trống của bác sĩ theo ngày
     */
    public List<DoctorTimeSlotDTO> getAvailableTimeSlots(Integer doctorId, LocalDate date) {
        // Tạo danh sách khung giờ mặc định từ 8h đến 16h
        List<DoctorTimeSlotDTO> timeSlots = createDefaultTimeSlots();
        
        // Mặc định tất cả các khung giờ là UNAVAILABLE
        for (DoctorTimeSlotDTO slot : timeSlots) {
            slot.setStatus(AvailabilityStatus.UNAVAILABLE);
        }
        
        // Lấy danh sách khả năng của bác sĩ trong ngày từ database (nếu có)
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(doctorId, date);
        
        // Cập nhật trạng thái khung giờ dựa trên khả năng của bác sĩ
        for (DoctorAvailability availability : availabilities) {
            if (availability.getStatus() == AvailabilityStatus.AVAILABLE) {
                LocalTime startTime = availability.getStartTime();
                LocalTime endTime = availability.getEndTime();
                
                // Cập nhật tất cả các khung giờ nằm trong khoảng thời gian làm việc
                for (DoctorTimeSlotDTO slot : timeSlots) {
                    LocalTime slotStartTime = slot.getStartTime();
                    LocalTime slotEndTime = slot.getEndTime();
                    
                    // Nếu khung giờ nằm trong khoảng thời gian làm việc
                    if (!slotStartTime.isBefore(startTime) && !slotEndTime.isAfter(endTime)) {
                        slot.setStatus(AvailabilityStatus.AVAILABLE);
                    }
                }
            }
        }
        
        // Lấy danh sách lịch hẹn của bác sĩ trong ngày
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(
                doctorId, 
                date.atStartOfDay(), 
                date.atTime(23, 59, 59)
        );
        
        // Cập nhật trạng thái khung giờ dựa trên lịch hẹn đã đặt
        for (Appointment appointment : appointments) {
            LocalTime appointmentTime = appointment.getAppointmentTime().toLocalTime();
            
            // Tìm khung giờ tương ứng và đánh dấu là đã đặt
            for (DoctorTimeSlotDTO slot : timeSlots) {
                if (slot.getStartTime().equals(appointmentTime)) {
                    slot.setStatus(AvailabilityStatus.BOOKED);
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
            
            DoctorTimeSlotDTO slot = new DoctorTimeSlotDTO(startTime, endTime, AvailabilityStatus.AVAILABLE);
            timeSlots.add(slot);
        }
        
        return timeSlots;
    }
    
    /**
     * Kiểm tra bác sĩ có khả dụng trong khung giờ đặt lịch không
     */
    public boolean isDoctorAvailable(Integer doctorId, LocalDateTime appointmentTime) {
        LocalDate appointmentDate = appointmentTime.toLocalDate();
        LocalTime appointmentTimeOfDay = appointmentTime.toLocalTime();
        
        // Kiểm tra xem bác sĩ có lịch làm việc trong ngày và khung giờ đó không
        List<DoctorAvailability> availabilities = doctorAvailabilityRepository.findByDoctorIdAndDate(doctorId, appointmentDate);
        
        boolean isInWorkingHours = false;
        for (DoctorAvailability availability : availabilities) {
            if (availability.getStatus() == AvailabilityStatus.AVAILABLE &&
                !appointmentTimeOfDay.isBefore(availability.getStartTime()) &&
                !appointmentTimeOfDay.isAfter(availability.getEndTime())) {
                isInWorkingHours = true;
                break;
            }
        }
        
        // Nếu không nằm trong giờ làm việc, trả về false
        if (!isInWorkingHours) {
            return false;
        }
        
        // Kiểm tra xem bác sĩ đã có lịch hẹn trong khung giờ đó chưa
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentTime(doctorId, appointmentTime);
        
        // Nếu không có lịch hẹn nào, bác sĩ khả dụng
        return appointments.isEmpty();
    }
}
