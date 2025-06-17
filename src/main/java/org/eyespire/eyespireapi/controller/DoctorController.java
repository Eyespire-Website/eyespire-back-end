package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {
    
    @Autowired
    private DoctorService doctorService;
    
    /**
     * Lấy danh sách tất cả bác sĩ
     */
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }
    
    /**
     * Lấy thông tin chi tiết bác sĩ theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable Integer id) {
        return doctorService.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Lấy danh sách bác sĩ theo chuyên khoa
     */
    @GetMapping("/specialty/{specialtyId}")
    public ResponseEntity<List<Doctor>> getDoctorsBySpecialty(@PathVariable Integer specialtyId) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialty(specialtyId));
    }
    
    /**
     * Lấy danh sách khung giờ trống của bác sĩ theo ngày
     */
    @GetMapping("/{id}/available-slots")
    public ResponseEntity<List<DoctorTimeSlotDTO>> getAvailableTimeSlots(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(doctorService.getAvailableTimeSlots(id, date));
    }
    
    /**
     * Kiểm tra bác sĩ có khả dụng trong khung giờ cụ thể không
     */
    @GetMapping("/{id}/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkDoctorAvailability(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        LocalDateTime appointmentTime = LocalDateTime.of(date, time);
        boolean isAvailable = doctorService.isDoctorAvailable(id, appointmentTime);
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }
}
