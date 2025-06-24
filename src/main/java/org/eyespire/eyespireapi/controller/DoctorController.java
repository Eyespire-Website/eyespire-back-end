package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.DoctorDTO;
import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.service.AppointmentService;
import org.eyespire.eyespireapi.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {
    
    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

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
     * Lấy thông tin bác sĩ theo User ID
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<?> getDoctorByUserId(@PathVariable Integer userId) {
        return doctorService.getDoctorByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
    
    /**
     * Cập nhật thông tin bác sĩ
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Integer id, @RequestBody DoctorDTO doctorDTO) {
        try {
            Doctor updatedDoctor = doctorService.updateDoctor(id, doctorDTO);
            return ResponseEntity.ok(updatedDoctor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Lấy danh sách cuộc hẹn của bác sĩ dựa trên userId
    @GetMapping("/by-user/{userId}/appointments")
    public ResponseEntity<?> getDoctorAppointmentsByUserId(@PathVariable Integer userId) {
        try {
            Integer doctorId = doctorService.getDoctorIdByUserId(userId);
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bác sĩ: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách cuộc hẹn: " + e.getMessage());
        }
    }
    @GetMapping("/by-user/{userId}/patients")
    public ResponseEntity<?> getPatientsByDoctor(@PathVariable Integer userId) {
        try {
            Integer doctorId = doctorService.getDoctorIdByUserId(userId);
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
            List<Object> patients = appointments.stream()
                    .map(appointment -> appointment.getPatient())
                    .distinct()
                    .map(user -> new Object() {
                        public final Integer id = user.getId();
                        public final String name = user.getName();
                        public final String phone = user.getPhone();
                        public final String email = user.getEmail();
                    })
                    .collect(Collectors.toList());
            return ResponseEntity.ok(patients);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bác sĩ: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách bệnh nhân: " + e.getMessage());
        }
    }
}
