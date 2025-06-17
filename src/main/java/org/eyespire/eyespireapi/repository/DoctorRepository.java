package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Integer> {
    
    // Tìm bác sĩ theo chuyên khoa
    List<Doctor> findBySpecialtyId(Integer specialtyId);
    
    // Tìm bác sĩ theo tên chứa từ khóa
    List<Doctor> findByNameContainingIgnoreCase(String keyword);
    
    // Tìm bác sĩ theo chuyên khoa và tên chứa từ khóa
    List<Doctor> findBySpecialtyIdAndNameContainingIgnoreCase(Integer specialtyId, String keyword);
}
