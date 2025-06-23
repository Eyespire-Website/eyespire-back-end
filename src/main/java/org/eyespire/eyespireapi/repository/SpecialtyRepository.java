package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {
    
    // Tìm chuyên khoa theo tên chứa từ khóa
    List<Specialty> findByNameContainingIgnoreCase(String keyword);
}
