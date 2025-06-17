package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.service.AppointmentService;
import org.eyespire.eyespireapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "http://localhost:3000")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private UserService userService;

    // Đặt lịch khám mới
    @PostMapping
    public ResponseEntity<?> bookAppointment(@RequestBody AppointmentDTO appointmentDTO) {
        try {
            Appointment appointment = appointmentService.createAppointment(appointmentDTO);
            return ResponseEntity.ok(appointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Không thể đặt lịch khám: " + e.getMessage());
        }
    }

    // Lấy danh sách lịch hẹn của bệnh nhân
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Integer patientId) {
        try {
            User patient = userService.getUserById(patientId);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy bệnh nhân");
            }
            
            List<Appointment> appointments = appointmentService.getAppointmentsByPatient(patientId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    // Lấy danh sách lịch hẹn của bác sĩ
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAppointments(@PathVariable Integer doctorId) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    // Lấy chi tiết lịch hẹn
    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Integer id) {
        try {
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }
            return ResponseEntity.ok(appointmentOpt.get());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin lịch hẹn: " + e.getMessage());
        }
    }

    // Hủy lịch hẹn
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAppointment(@PathVariable Integer id) {
        try {
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }
            
            Appointment appointment = appointmentOpt.get();
            // Chỉ cho phép hủy lịch hẹn đang ở trạng thái PENDING hoặc CONFIRMED
            if (appointment.getStatus() != AppointmentStatus.PENDING && 
                appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Không thể hủy lịch hẹn ở trạng thái " + appointment.getStatus());
            }
            
            Appointment updatedAppointment = appointmentService.cancelAppointment(id);
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi hủy lịch hẹn: " + e.getMessage());
        }
    }

    // Cập nhật trạng thái lịch hẹn
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Thiếu thông tin trạng thái");
            }
            
            AppointmentStatus status;
            try {
                status = AppointmentStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Trạng thái không hợp lệ");
            }
            
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);
            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }
            
            Appointment updatedAppointment = appointmentService.updateAppointmentStatus(id, status);
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái lịch hẹn: " + e.getMessage());
        }
    }
}
