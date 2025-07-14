package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.MedicalRecord;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {
    List<MedicalRecord> findByDoctorId(Integer doctorId);
    List<MedicalRecord> findByPatientId(Integer patientId);
    
    // Lọc hồ sơ bệnh án theo patientId và trạng thái cuộc hẹn COMPLETED
    @Query("SELECT mr FROM MedicalRecord mr WHERE mr.patient.id = :patientId AND mr.appointment.status = :status")
    List<MedicalRecord> findByPatientIdAndAppointmentStatus(@Param("patientId") Integer patientId, @Param("status") AppointmentStatus status);
    
    boolean existsByAppointmentId(Integer appointmentId);
    java.util.Optional<MedicalRecord> findByAppointmentId(Integer appointmentId);
}