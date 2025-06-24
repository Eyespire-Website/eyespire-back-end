package org.eyespire.eyespireapi.service;

import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.repository.DoctorAvailabilityRepository;
import org.eyespire.eyespireapi.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class DoctorAvailabilityService {

    @Autowired
    private DoctorAvailabilityRepository doctorAvailabilityRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    /**
     * Lấy tất cả lịch làm việc
     */
    public List<DoctorAvailability> findAll() {
        return doctorAvailabilityRepository.findAll();
    }

    /**
     * Lấy lịch làm việc theo ID
     */
    public Optional<DoctorAvailability> findById(Integer id) {
        return doctorAvailabilityRepository.findById(id);
    }

    /**
     * Lấy lịch làm việc theo bác sĩ và ngày
     */
    public List<DoctorAvailability> findByDoctorIdAndDate(Integer doctorId, LocalDate date) {
        return doctorAvailabilityRepository.findByDoctorIdAndDate(doctorId, date);
    }

    /**
     * Lấy lịch làm việc theo bác sĩ, ngày và trạng thái
     */
    public List<DoctorAvailability> findByDoctorIdAndDateAndStatus(Integer doctorId, LocalDate date, AvailabilityStatus status) {
        return doctorAvailabilityRepository.findByDoctorIdAndDateAndStatus(doctorId, date, status);
    }

    /**
     * Lấy lịch làm việc theo bác sĩ và khoảng thời gian
     */
    public List<DoctorAvailability> findByDoctorIdAndDateBetween(Integer doctorId, LocalDate startDate, LocalDate endDate) {
        return doctorAvailabilityRepository.findByDoctorIdAndDateBetween(doctorId, startDate, endDate);
    }

    /**
     * Lấy lịch làm việc theo ngày
     */
    public List<DoctorAvailability> findByDate(LocalDate date) {
        return doctorAvailabilityRepository.findByDate(date);
    }

    /**
     * Lấy lịch làm việc trong khoảng thời gian
     */
    public List<DoctorAvailability> findByDateBetween(LocalDate startDate, LocalDate endDate) {
        return doctorAvailabilityRepository.findByDateBetween(startDate, endDate);
    }

    /**
     * Lấy lịch làm việc theo ngày và trạng thái
     */
    public List<DoctorAvailability> findByDateAndStatus(LocalDate date, AvailabilityStatus status) {
        return doctorAvailabilityRepository.findByDateAndStatus(date, status);
    }

    /**
     * Tạo mới lịch làm việc
     */
    public DoctorAvailability create(DoctorAvailability availability) {
        // Kiểm tra bác sĩ có tồn tại không
        Doctor doctor = doctorRepository.findById(availability.getDoctor().getId())
                .orElseThrow(() -> new IllegalArgumentException("Bác sĩ không tồn tại"));
        
        // Kiểm tra trùng lịch
        List<DoctorAvailability> overlappingAvailabilities = doctorAvailabilityRepository.findOverlappingAvailabilities(
                doctor.getId(), 
                availability.getDate(), 
                availability.getStartTime(), 
                availability.getEndTime());
        
        if (!overlappingAvailabilities.isEmpty()) {
            throw new IllegalArgumentException("Đã tồn tại lịch làm việc trong khoảng thời gian này");
        }
        
        availability.setDoctor(doctor);
        return doctorAvailabilityRepository.save(availability);
    }

    /**
     * Cập nhật lịch làm việc
     */
    public DoctorAvailability update(Integer id, DoctorAvailability updatedAvailability) {
        DoctorAvailability availability = doctorAvailabilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch làm việc không tồn tại"));
        
        // Kiểm tra trùng lịch nếu thay đổi thời gian
        if (updatedAvailability.getDate() != null || 
            updatedAvailability.getStartTime() != null || 
            updatedAvailability.getEndTime() != null) {
            
            LocalDate date = updatedAvailability.getDate() != null ? 
                    updatedAvailability.getDate() : availability.getDate();
            LocalTime startTime = updatedAvailability.getStartTime() != null ? 
                    updatedAvailability.getStartTime() : availability.getStartTime();
            LocalTime endTime = updatedAvailability.getEndTime() != null ? 
                    updatedAvailability.getEndTime() : availability.getEndTime();
            
            List<DoctorAvailability> overlappingAvailabilities = doctorAvailabilityRepository.findOverlappingAvailabilities(
                    availability.getDoctor().getId(), date, startTime, endTime);
            
            // Loại bỏ chính nó khỏi danh sách trùng lịch
            overlappingAvailabilities.removeIf(a -> a.getId().equals(id));
            
            if (!overlappingAvailabilities.isEmpty()) {
                throw new IllegalArgumentException("Đã tồn tại lịch làm việc trong khoảng thời gian này");
            }
        }
        
        // Cập nhật thông tin
        if (updatedAvailability.getDate() != null) {
            availability.setDate(updatedAvailability.getDate());
        }
        if (updatedAvailability.getStartTime() != null) {
            availability.setStartTime(updatedAvailability.getStartTime());
        }
        if (updatedAvailability.getEndTime() != null) {
            availability.setEndTime(updatedAvailability.getEndTime());
        }
        if (updatedAvailability.getStatus() != null) {
            availability.setStatus(updatedAvailability.getStatus());
        }
        if (updatedAvailability.getNotes() != null) {
            availability.setNotes(updatedAvailability.getNotes());
        }
        
        return doctorAvailabilityRepository.save(availability);
    }

    /**
     * Cập nhật trạng thái lịch làm việc
     */
    public DoctorAvailability updateStatus(Integer id, AvailabilityStatus status) {
        DoctorAvailability availability = doctorAvailabilityRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Lịch làm việc không tồn tại"));
        
        availability.setStatus(status);
        return doctorAvailabilityRepository.save(availability);
    }

    /**
     * Xóa lịch làm việc
     */
    public void delete(Integer id) {
        doctorAvailabilityRepository.deleteById(id);
    }
}
