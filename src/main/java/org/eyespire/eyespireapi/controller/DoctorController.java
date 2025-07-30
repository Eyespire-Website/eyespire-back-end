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
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = {"https://eyespire.vercel.app", "https://eyespire.vercel.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentService appointmentService;

    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Doctor> getDoctorById(@PathVariable Integer id) {
        return doctorService.getDoctorById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/specialty/{specialtyId}")
    public ResponseEntity<List<Doctor>> getDoctorsBySpecialty(@PathVariable Integer specialtyId) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialty(specialtyId));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<Doctor> getDoctorByUserId(@PathVariable Integer userId) {
        return doctorService.getDoctorByUserId(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @GetMapping("/{id}/available-slots")
    public ResponseEntity<List<DoctorTimeSlotDTO>> getAvailableTimeSlots(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer excludeAppointmentId) {
        return ResponseEntity.ok(doctorService.getAvailableTimeSlots(id, date, excludeAppointmentId));
    }

    @GetMapping("/{id}/check-availability")
    public ResponseEntity<Map<String, Boolean>> checkDoctorAvailability(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        LocalDateTime appointmentTime = LocalDateTime.of(date, time);
        boolean isAvailable = doctorService.isDoctorAvailable(id, appointmentTime);
        return ResponseEntity.ok(Map.of("available", isAvailable));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Doctor>> getFeaturedDoctors() {
        List<Doctor> allDoctors = doctorService.getAllDoctors();
        // Lấy tối đa 6 bác sĩ đầu tiên làm bác sĩ nổi bật
        List<Doctor> featuredDoctors = allDoctors.stream()
                .limit(6)
                .collect(Collectors.toList());
        return ResponseEntity.ok(featuredDoctors);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctor(@PathVariable Integer id, @RequestBody DoctorDTO doctorDTO) {
        try {
            // Kiểm tra id trong DTO khớp với id trong đường dẫn
            if (!id.equals(doctorDTO.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("ID trong đường dẫn phải khớp với id trong DTO");
            }
            // Kiểm tra Doctor tồn tại và userId khớp
            Optional<Doctor> existingDoctor = doctorService.getDoctorById(id);
            if (existingDoctor.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Bác sĩ không tồn tại");
            }
            if (!existingDoctor.get().getUserId().equals(doctorDTO.getUserId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("userId trong DTO không khớp với userId của bác sĩ");
            }
            Doctor updatedDoctor = doctorService.updateDoctor(id, doctorDTO);
            return ResponseEntity.ok(updatedDoctor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật bác sĩ: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createDoctor(@RequestBody DoctorDTO doctorDTO) {
        try {
            Doctor createdDoctor = doctorService.createDoctor(doctorDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDoctor);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi tạo bác sĩ: " + e.getMessage());
        }
    }

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
    
    /**
     * Lấy danh sách bác sĩ có sẵn theo ngày và giờ
     */
    @GetMapping("/available")
    public ResponseEntity<?> getAvailableDoctors(
            @RequestParam String date,
            @RequestParam(required = false) String time) {
        try {
            LocalDate appointmentDate = LocalDate.parse(date);
            List<Doctor> allDoctors = doctorService.getAllDoctors();
            
            if (time != null && !time.isEmpty()) {
                // Lọc doctors available cho ngày và giờ cụ thể
                LocalTime appointmentTime = LocalTime.parse(time);
                LocalDateTime appointmentDateTime = LocalDateTime.of(appointmentDate, appointmentTime);
                
                List<Doctor> availableDoctors = allDoctors.stream()
                    .filter(doctor -> doctorService.isDoctorAvailable(doctor.getId(), appointmentDateTime))
                    .collect(Collectors.toList());
                    
                return ResponseEntity.ok(availableDoctors);
            } else {
                // Trả về tất cả doctors nếu không có time parameter
                return ResponseEntity.ok(allDoctors);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách bác sĩ: " + e.getMessage());
        }
    }
}