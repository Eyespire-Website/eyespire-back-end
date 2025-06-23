package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.model.Specialty;
import org.eyespire.eyespireapi.repository.SpecialtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/specialties")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SpecialtyController {

    @Autowired
    private SpecialtyRepository specialtyRepository;
    
    // Lấy danh sách tất cả chuyên khoa
    @GetMapping
    public ResponseEntity<List<Specialty>> getAllSpecialties() {
        List<Specialty> specialties = specialtyRepository.findAll();
        return ResponseEntity.ok(specialties);
    }
    
    // Lấy thông tin chuyên khoa theo ID
    @GetMapping("/{id}")
    public ResponseEntity<Specialty> getSpecialtyById(@PathVariable Integer id) {
        Optional<Specialty> specialty = specialtyRepository.findById(id);
        return specialty.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    // Tìm kiếm chuyên khoa theo từ khóa
    @GetMapping("/search")
    public ResponseEntity<List<Specialty>> searchSpecialties(@RequestParam String keyword) {
        List<Specialty> specialties = specialtyRepository.findByNameContainingIgnoreCase(keyword);
        return ResponseEntity.ok(specialties);
    }
}
