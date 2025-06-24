package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.AppointmentDTO;
import org.eyespire.eyespireapi.dto.DoctorTimeSlotDTO;
import org.eyespire.eyespireapi.dto.UserDTO;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.User;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.service.AppointmentService;
import org.eyespire.eyespireapi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể đặt lịch khám: " + e.getMessage());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    // Lấy danh sách lịch hẹn của bác sĩ
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAppointments(@PathVariable Integer doctorId) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctor(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy thông tin lịch hẹn: " + e.getMessage());
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
            if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Không thể hủy lịch hẹn ở trạng thái " + appointment.getStatus());
            }

            Appointment updatedAppointment = appointmentService.cancelAppointment(id);
            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi hủy lịch hẹn: " + e.getMessage());
        }
    }

    // Cập nhật trạng thái lịch hẹn
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Integer id, @RequestBody Map<String, String> statusUpdate) {
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật trạng thái lịch hẹn: " + e.getMessage());
        }
    }

    // Cập nhật thông tin lịch hẹn
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Integer id, @RequestBody AppointmentDTO appointmentDTO) {
        try {
            Optional<Appointment> appointmentOpt = appointmentService.getAppointmentById(id);

            if (!appointmentOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy lịch hẹn");
            }

            Appointment appointment = appointmentOpt.get();

            // Chỉ cho phép cập nhật nếu trạng thái là PENDING
            if (appointment.getStatus() != AppointmentStatus.PENDING) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Chỉ có thể cập nhật lịch hẹn ở trạng thái PENDING");
            }

            Appointment updatedAppointment = appointmentService.updateAppointment(id, appointmentDTO);

            return ResponseEntity.ok(updatedAppointment);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Lỗi khi cập nhật lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/available-by-date")
    public ResponseEntity<?> getAvailableTimeSlotsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DoctorTimeSlotDTO> availableSlots = appointmentService.getAvailableTimeSlotsByDate(date);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy khung giờ trống theo ngày: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAppointments() {
        try {
            List<AppointmentDTO> appointments = appointmentService.getAllAppointments().stream().map(this::convertToDTO).collect(Collectors.toList());

            if (appointments.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Không có lịch hẹn nào được tìm thấy");
            }

            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lấy danh sách lịch hẹn: " + e.getMessage());
        }
    }

    @GetMapping("/by-date")
    public ResponseEntity<?> getAppointmentsByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDate(date);
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi lấy danh sách cuộc hẹn theo ngày: " + e.getMessage());
        }
    }

    @GetMapping("/by-doctor-and-date")
    public ResponseEntity<?> getAppointmentsByDoctorAndDate(
            @RequestParam Integer doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Appointment> appointments = appointmentService.getAppointmentsByDoctorAndDate(doctorId, date);
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(appointmentDTOs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi lấy danh sách cuộc hẹn theo bác sĩ và ngày: " + e.getMessage());
        }
    }

    // Chuyển đổi từ Appointment sang AppointmentDTO
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();

        dto.setId(appointment.getId());

        // Bảo vệ null cho patient
        dto.setUserId(appointment.getPatient() != null ? appointment.getPatient().getId() : null);

        // Bảo vệ null cho doctor và service
        dto.setDoctorId(appointment.getDoctor() != null ? appointment.getDoctor().getId() : null);
        dto.setServiceId(appointment.getService() != null ? appointment.getService().getId() : null);

        // Định dạng ngày giờ cho front-end, nếu appointmentTime không null
        if (appointment.getAppointmentTime() != null) {
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            dto.setAppointmentDate(appointment.getAppointmentTime().format(dateFormatter));
            dto.setTimeSlot(appointment.getAppointmentTime().format(timeFormatter));
        } else {
            dto.setAppointmentDate(null);
            dto.setTimeSlot(null);
        }

        dto.setPatientName(appointment.getPatientName());
        dto.setPatientEmail(appointment.getPatientEmail());
        dto.setPatientPhone(appointment.getPatientPhone());
        dto.setNotes(appointment.getNotes());
        dto.setStatus(appointment.getStatus() != null ? appointment.getStatus().name() : null);

        // Nếu có thông tin bệnh nhân, chuyển đổi sang UserDTO
        if (appointment.getPatient() != null) {
            UserDTO patientDTO = new UserDTO();
            patientDTO.setId(appointment.getPatient().getId());
            patientDTO.setName(appointment.getPatient().getName());
            patientDTO.setEmail(appointment.getPatient().getEmail());
            patientDTO.setPhone(appointment.getPatient().getPhone());
            patientDTO.setProvince(appointment.getPatient().getProvince());
            patientDTO.setDistrict(appointment.getPatient().getDistrict());
            patientDTO.setWard(appointment.getPatient().getWard());
            patientDTO.setAddressDetail(appointment.getPatient().getAddressDetail());

            // ⚠️ Fix lỗi copy paste: village không phải addressDetail
            patientDTO.setVillage(appointment.getPatient().getAddressDetail());

            patientDTO.setGender(appointment.getPatient().getGender() != null
                    ? appointment.getPatient().getGender().toString()
                    : null);

            patientDTO.setDateOfBirth(appointment.getPatient().getDateOfBirth() != null
                    ? appointment.getPatient().getDateOfBirth().toString()
                    : null);

            dto.setPatient(patientDTO);
        }

        return dto;
    }
}

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
class AvailableSlotsController {
    
    @Autowired
    private AppointmentService appointmentService;
    
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlotsForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DoctorTimeSlotDTO> availableSlots = appointmentService.getAvailableTimeSlotsByDate(date);
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy khung giờ trống theo ngày: " + e.getMessage());
        }
    }
}