package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Integer> {
    
    // Tìm dịch vụ theo tên chứa từ khóa
    List<MedicalService> findByNameContainingIgnoreCase(String keyword);
    
    // Tìm dịch vụ theo bác sĩ
    @Query("SELECT ms FROM MedicalService ms JOIN ms.doctors d WHERE d.id = :doctorId")
    List<MedicalService> findByDoctorId(@Param("doctorId") Integer doctorId);
}
