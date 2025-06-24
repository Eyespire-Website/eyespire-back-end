package org.eyespire.eyespireapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.MedicalRecord;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.service.AppointmentService;
import org.eyespire.eyespireapi.service.MedicalRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/medical-records")
    public ResponseEntity<?> createMedicalRecord(
            @RequestParam("patientId") Integer patientId,
            @RequestParam("doctorId") Integer doctorId,
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "appointmentId", required = false) Integer appointmentId,
            @RequestParam(value = "productQuantities", required = false) String productQuantitiesJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        try {
            System.out.println("Received POST /medical-records: patientId=" + patientId + ", doctorId=" + doctorId +
                    ", diagnosis=" + diagnosis + ", appointmentId=" + appointmentId + ", productQuantities=" + productQuantitiesJson +
                    ", files=" + (files != null ? Arrays.stream(files).map(MultipartFile::getOriginalFilename).toList() : "none"));

            // Validate patientId
            if (patientId == null || patientId <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID bệnh nhân không hợp lệ!"));
            }

            // Validate doctorId
            if (doctorId == null || doctorId <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID bác sĩ không hợp lệ!"));
            }

            // Validate diagnosis
            if (diagnosis == null || diagnosis.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Chẩn đoán không được để trống!"));
            }

            // Validate appointmentId
            if (appointmentId != null) {
                Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Cuộc hẹn không tồn tại!"));
                System.out.println("Appointment status for ID " + appointmentId + ": " + appointment.getStatus().name());
                if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Cuộc hẹn chưa hoàn thành, không thể tạo hồ sơ!"));
                }
            }

            // Validate productQuantities
            List<Map<String, Integer>> productQuantities = null;
            if (productQuantitiesJson != null && !productQuantitiesJson.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    productQuantities = mapper.readValue(productQuantitiesJson,
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    for (Map<String, Integer> pq : productQuantities) {
                        if (!pq.containsKey("productId") || !pq.containsKey("quantity") ||
                                pq.get("productId") <= 0 || pq.get("quantity") <= 0) {
                            return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không hợp lệ!"));
                        }
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không đúng định dạng JSON!"));
                }
            }

            // Validate files
            if (files != null) {
                for (MultipartFile file : files) {
                    if (file.isEmpty()) {
                        return ResponseEntity.badRequest().body(new ErrorResponse("File rỗng: " + file.getOriginalFilename()));
                    }
                    if (file.getSize() > 5 * 1024 * 1024) {
                        return ResponseEntity.badRequest().body(new ErrorResponse("File quá lớn: " + file.getOriginalFilename()));
                    }
                    String contentType = file.getContentType();
                    if (!Arrays.asList("image/jpeg", "image/png", "application/pdf").contains(contentType)) {
                        return ResponseEntity.badRequest().body(new ErrorResponse("Định dạng file không được hỗ trợ: " + file.getOriginalFilename()));
                    }
                }
            }

            MedicalRecord medicalRecord = medicalRecordService.createMedicalRecord(
                    patientId, doctorId, diagnosis, notes, appointmentId, productQuantities, files);
            return new ResponseEntity<>(medicalRecord, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            System.err.println("Bad Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Internal Server Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/products-medicine")
    public ResponseEntity<?> getProductsMedicine() {
        try {
            System.out.println("Received GET /medical-records/products-medicine");
            List<Product> products = medicalRecordService.getProductsMedicine();
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error fetching products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi lấy danh sách thuốc: " + e.getMessage()));
        }
    }

    @GetMapping("/appointments/{appointmentId}/status")
    public ResponseEntity<?> getAppointmentStatus(@PathVariable Integer appointmentId) {
        try {
            System.out.println("Received GET /appointments/" + appointmentId + "/status");
            Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc hẹn không tồn tại!"));
            return ResponseEntity.ok(new StatusResponse(appointment.getStatus().name()));
        } catch (IllegalArgumentException e) {
            System.err.println("Bad Request: " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error fetching appointment status: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorMedicalRecords(@PathVariable Integer doctorId) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            String token = SecurityContextHolder.getContext().getAuthentication().getCredentials().toString();
            System.out.println("Received GET /medical-records/doctor/" + doctorId + " | User: " + username +
                    " | Token: " + (token != null ? token.substring(0, Math.min(10, token.length())) + "..." : "No token"));

            // Validate doctorId
            if (doctorId == null || doctorId <= 0) {
                System.err.println("Invalid doctorId: " + doctorId);
                return ResponseEntity.badRequest().body(new ErrorResponse("ID bác sĩ không hợp lệ!"));
            }

            List<MedicalRecord> records = medicalRecordService.getDoctorMedicalRecordsByUserId(doctorId);
            System.out.println("Query executed for doctorId: " + doctorId + " | Found: " + records.size() + " records");
            // Log only essential details to avoid toString recursion
            String recordsSummary = records.isEmpty() ? "Empty" :
                    records.stream()
                            .map(record -> "MedicalRecord{id=" + record.getId() + ", patientId=" +
                                    (record.getPatient() != null ? record.getPatient().getId() : "null") +
                                    ", diagnosis=" + record.getDiagnosis() + "}")
                            .collect(Collectors.joining(", "));
            System.out.println("Records: " + recordsSummary);

            return ResponseEntity.ok(records);
        } catch (IllegalArgumentException e) {
            System.err.println("Bad Request for doctorId " + doctorId + ": " + e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error fetching medical records for doctorId " + doctorId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/by-appointment/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> checkMedicalRecordExistsByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            System.out.println("Received request for /by-appointment/" + appointmentId + " by user: " +
                    SecurityContextHolder.getContext().getAuthentication().getName());
            boolean exists = medicalRecordService.existsByAppointmentId(appointmentId);
            System.out.println("Medical record exists for appointmentId " + appointmentId + ": " + exists);
            return ResponseEntity.ok(new ExistsResponse(exists));
        } catch (Exception e) {
            System.err.println("Error checking medical record for appointmentId " + appointmentId + ": " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server: " + e.getMessage()));
        }
    }

    private static class ExistsResponse {
        private final boolean exists;

        public ExistsResponse(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }
    }

    private static class ErrorResponse {
        private final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class StatusResponse {
        private final String status;

        public StatusResponse(String status) {
            this.status = status;
        }

        public String getStatus() {
            return status;
        }
    }
}