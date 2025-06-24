package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Integer> {
    List<MedicalRecord> findByDoctorId(Integer doctorId);
    boolean existsByAppointmentId(Integer appointmentId);
}