package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.DoctorAvailabilityDTO;
import org.eyespire.eyespireapi.model.Doctor;
import org.eyespire.eyespireapi.model.DoctorAvailability;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;
import org.eyespire.eyespireapi.service.DoctorAvailabilityService;
import org.eyespire.eyespireapi.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/doctor-availabilities")
@CrossOrigin(origins = "*")
public class DoctorAvailabilityController {

    @Autowired
    private DoctorAvailabilityService doctorAvailabilityService;
    
    @Autowired
    private DoctorService doctorService;

    /**
     * Lấy tất cả lịch làm việc của bác sĩ
     */
    @GetMapping
    public ResponseEntity<?> getAllDoctorAvailabilities() {
        try {
            List<DoctorAvailability> availabilities = doctorAvailabilityService.findAll();
            List<DoctorAvailabilityDTO> dtos = availabilities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch làm việc theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorAvailabilityById(@PathVariable Integer id) {
        try {
            return doctorAvailabilityService.findById(id)
                    .map(availability -> ResponseEntity.ok(convertToDTO(availability)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy thông tin lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch làm việc theo bác sĩ và ngày
     */
    @GetMapping("/by-doctor-and-date")
    public ResponseEntity<?> getDoctorAvailabilitiesByDoctorAndDate(
            @RequestParam Integer doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DoctorAvailability> availabilities = doctorAvailabilityService.findByDoctorIdAndDate(doctorId, date);
            List<DoctorAvailabilityDTO> dtos = availabilities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy lịch làm việc của bác sĩ: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch làm việc theo ngày
     */
    @GetMapping("/by-date")
    public ResponseEntity<?> getDoctorAvailabilitiesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<DoctorAvailability> availabilities = doctorAvailabilityService.findByDate(date);
            List<DoctorAvailabilityDTO> dtos = availabilities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy lịch làm việc theo ngày: " + e.getMessage());
        }
    }

    /**
     * Lấy lịch làm việc theo khoảng thời gian
     */
    @GetMapping("/by-date-range")
    public ResponseEntity<?> getDoctorAvailabilitiesByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<DoctorAvailability> availabilities = doctorAvailabilityService.findByDateBetween(startDate, endDate);
            List<DoctorAvailabilityDTO> dtos = availabilities.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy lịch làm việc trong khoảng thời gian: " + e.getMessage());
        }
    }

    /**
     * Tạo mới lịch làm việc
     */
    @PostMapping
    public ResponseEntity<?> createDoctorAvailability(@RequestBody DoctorAvailabilityDTO availabilityDTO) {
        try {
            DoctorAvailability availability = convertToEntity(availabilityDTO);
            DoctorAvailability createdAvailability = doctorAvailabilityService.create(availability);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(createdAvailability));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi tạo lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Cập nhật lịch làm việc
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDoctorAvailability(
            @PathVariable Integer id, 
            @RequestBody DoctorAvailabilityDTO availabilityDTO) {
        try {
            DoctorAvailability availability = convertToEntity(availabilityDTO);
            DoctorAvailability updatedAvailability = doctorAvailabilityService.update(id, availability);
            return ResponseEntity.ok(convertToDTO(updatedAvailability));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi khi cập nhật lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Cập nhật trạng thái lịch làm việc
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateDoctorAvailabilityStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> statusUpdate) {
        try {
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest().body("Trạng thái không được để trống");
            }
            
            AvailabilityStatus status = AvailabilityStatus.valueOf(statusStr);
            DoctorAvailability updatedAvailability = doctorAvailabilityService.updateStatus(id, status);
            return ResponseEntity.ok(convertToDTO(updatedAvailability));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Trạng thái không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Xóa lịch làm việc
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDoctorAvailability(@PathVariable Integer id) {
        try {
            doctorAvailabilityService.delete(id);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi xóa lịch làm việc: " + e.getMessage());
        }
    }

    /**
     * Chuyển đổi từ Entity sang DTO
     */
    private DoctorAvailabilityDTO convertToDTO(DoctorAvailability availability) {
        DoctorAvailabilityDTO dto = new DoctorAvailabilityDTO();
        dto.setId(availability.getId());
        dto.setDoctorId(availability.getDoctor().getId());
        dto.setDoctorName(availability.getDoctor().getName());
        dto.setDate(availability.getDate());
        dto.setStartTime(availability.getStartTime());
        dto.setEndTime(availability.getEndTime());
        dto.setStatus(availability.getStatus());
        dto.setNotes(availability.getNotes());
        return dto;
    }

    /**
     * Chuyển đổi từ DTO sang Entity
     */
    private DoctorAvailability convertToEntity(DoctorAvailabilityDTO dto) {
        DoctorAvailability availability = new DoctorAvailability();
        if (dto.getId() != null) {
            availability.setId(dto.getId());
        }
        
        Doctor doctor = new Doctor();
        doctor.setId(dto.getDoctorId());
        availability.setDoctor(doctor);
        
        availability.setDate(dto.getDate());
        availability.setStartTime(dto.getStartTime());
        availability.setEndTime(dto.getEndTime());
        availability.setStatus(dto.getStatus());
        availability.setNotes(dto.getNotes());
        
        return availability;
    }
}
