package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.service.MedicalServiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-services")
@CrossOrigin(origins = "*")
public class MedicalServiceController {
    
    @Autowired
    private MedicalServiceService medicalServiceService;
    
    /**
     * Lấy danh sách tất cả dịch vụ y tế
     */
    @GetMapping
    public ResponseEntity<List<MedicalService>> getAllMedicalServices() {
        return ResponseEntity.ok(medicalServiceService.getAllMedicalServices());
    }
    
    /**
     * Lấy thông tin chi tiết dịch vụ y tế theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicalServiceById(@PathVariable Integer id) {
        MedicalService service = medicalServiceService.getMedicalServiceById(id);
        if (service == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(service);
    }
    
    /**
     * Lấy danh sách dịch vụ y tế theo loại
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MedicalService>> getMedicalServicesByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(medicalServiceService.getMedicalServicesByCategory(categoryId));
    }
    
    /**
     * Lấy danh sách dịch vụ y tế của bác sĩ
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MedicalService>> getMedicalServicesByDoctor(@PathVariable Integer doctorId) {
        return ResponseEntity.ok(medicalServiceService.getMedicalServicesByDoctor(doctorId));
    }
}
