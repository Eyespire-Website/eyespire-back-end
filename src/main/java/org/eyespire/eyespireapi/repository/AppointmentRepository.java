package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Integer> {
    
    // Tìm lịch hẹn theo bệnh nhân, sắp xếp theo thời gian giảm dần
    List<Appointment> findByPatientOrderByAppointmentTimeDesc(User patient);
    
    // Tìm lịch hẹn theo ID bệnh nhân, sắp xếp theo thời gian giảm dần
    List<Appointment> findByPatientIdOrderByAppointmentTimeDesc(Integer patientId);
    
    // Tìm lịch hẹn theo ID bác sĩ, sắp xếp theo thời gian giảm dần
    List<Appointment> findByDoctorIdOrderByAppointmentTimeDesc(Integer doctorId);
    
    // Tìm lịch hẹn trong khoảng thời gian
    List<Appointment> findByAppointmentTimeBetween(LocalDateTime start, LocalDateTime end);
    
    // Tìm lịch hẹn của bác sĩ trong khoảng thời gian
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(Integer doctorId, LocalDateTime start, LocalDateTime end);
    
    // Tìm lịch hẹn của bác sĩ trong khoảng thời gian và không ở trạng thái đã hủy
    List<Appointment> findByDoctorIdAndAppointmentTimeBetweenAndStatusNot(
            Integer doctorId, LocalDateTime start, LocalDateTime end, AppointmentStatus status);
    
    // Tìm lịch hẹn theo trạng thái
    List<Appointment> findByStatus(AppointmentStatus status);
    
    // Tìm lịch hẹn của bác sĩ theo trạng thái
    List<Appointment> findByDoctorIdAndStatus(Integer doctorId, AppointmentStatus status);
    
    // Tìm lịch hẹn của bệnh nhân theo trạng thái
    List<Appointment> findByPatientAndStatus(User patient, AppointmentStatus status);
    
    // Tìm lịch hẹn của bệnh nhân theo ID và trạng thái
    List<Appointment> findByPatientIdAndStatus(Integer patientId, AppointmentStatus status);
    
    // Tìm lịch hẹn của bác sĩ theo thời gian cụ thể
    List<Appointment> findByDoctorIdAndAppointmentTime(Integer doctorId, LocalDateTime appointmentTime);

    List<Appointment> findByDoctorIdAndAppointmentTimeBetweenOrderByAppointmentTimeAsc(Integer doctorId, LocalDateTime startOfDay, LocalDateTime endOfDay);

    List<Appointment> findByAppointmentTimeBetweenOrderByAppointmentTimeAsc(LocalDateTime startOfDay, LocalDateTime endOfDay);
    
    // Tìm lịch hẹn trong khoảng thời gian và không ở trạng thái đã hủy
    List<Appointment> findByAppointmentTimeBetweenAndStatusNot(
            LocalDateTime start, LocalDateTime end, AppointmentStatus status);
    
    // Tìm lịch hẹn theo ID thanh toán
    List<Appointment> findByPaymentId(Integer paymentId);
    
    // Tìm lịch hẹn đang chờ thanh toán
    List<Appointment> findByStatusOrderByAppointmentTimeDesc(AppointmentStatus status);
    
    // Tìm lịch hẹn đang chờ thanh toán của bác sĩ
    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentTimeDesc(Integer doctorId, AppointmentStatus status);
}
