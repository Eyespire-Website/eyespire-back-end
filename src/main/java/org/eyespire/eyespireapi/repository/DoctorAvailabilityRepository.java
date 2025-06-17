package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface DoctorAvailabilityRepository extends JpaRepository<DoctorAvailability, Integer> {
    
    // Tìm khả năng của bác sĩ theo ngày
    List<DoctorAvailability> findByDoctorIdAndDate(Integer doctorId, LocalDate date);
    
    // Tìm khả năng của bác sĩ theo ngày và trạng thái
    List<DoctorAvailability> findByDoctorIdAndDateAndStatus(Integer doctorId, LocalDate date, AvailabilityStatus status);
    
    // Tìm khả năng của bác sĩ trong khoảng thời gian
    List<DoctorAvailability> findByDoctorIdAndDateBetween(Integer doctorId, LocalDate startDate, LocalDate endDate);
    
    // Tìm khả năng của bác sĩ theo ngày và khung giờ
    List<DoctorAvailability> findByDoctorIdAndDateAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            Integer doctorId, LocalDate date, LocalTime startTime, LocalTime endTime);
            
    // Tìm khả năng của bác sĩ theo ngày cụ thể
    List<DoctorAvailability> findByDoctorIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
            Integer doctorId, LocalDate date, LocalTime appointmentTime, LocalTime appointmentTime2);
}
