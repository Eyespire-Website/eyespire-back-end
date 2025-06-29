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
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> createMedicalRecord(
            @RequestParam("patientId") Integer patientId,
            @RequestParam("doctorId") Integer doctorId,
            @RequestParam("diagnosis") String diagnosis,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "appointmentId", required = false) Integer appointmentId,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "productQuantities", required = false) String productQuantitiesJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {
        try {
            System.out.println("Received POST /medical-records: patientId=" + patientId + ", doctorId=" + doctorId +
                    ", diagnosis=" + diagnosis + ", appointmentId=" + appointmentId + ", serviceId=" + serviceId +
                    ", productQuantitiesJson=" + productQuantitiesJson +
                    ", files=" + (files != null ? Arrays.stream(files).map(MultipartFile::getOriginalFilename).collect(Collectors.toList()) : "none"));

            if (patientId == null || patientId <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID bệnh nhân không hợp lệ!"));
            }

            if (doctorId == null || doctorId <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID bác sĩ không hợp lệ!"));
            }

            if (diagnosis == null || diagnosis.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Chẩn đoán không được để trống!"));
            }

            if (appointmentId != null) {
                Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                        .orElseThrow(() -> new IllegalArgumentException("Cuộc hẹn không tồn tại!"));
                if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
                    return ResponseEntity.badRequest().body(new ErrorResponse("Cuộc hẹn chưa được xác nhận, không thể tạo hồ sơ!"));
                }
            }

            List<Map<String, Integer>> productQuantities = null;
            if (productQuantitiesJson != null && !productQuantitiesJson.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    productQuantities = mapper.readValue(productQuantitiesJson,
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    for (Map<String, Integer> pq : productQuantities) {
                        if (!pq.containsKey("productId") || !pq.containsKey("quantity") ||
                                pq.get("productId") == null || pq.get("productId") <= 0 ||
                                pq.get("quantity") == null || pq.get("quantity") <= 0) {
                            return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không hợp lệ!"));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing productQuantitiesJson: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không đúng định dạng JSON: " + e.getMessage()));
                }
            }

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
            if (appointmentId != null && serviceId != null) {
                appointmentService.updateAppointmentService(appointmentId, serviceId);
            }
            return new ResponseEntity<>(medicalRecord, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in createMedicalRecord: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in createMedicalRecord: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi tạo hồ sơ bệnh án: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/products-medicine")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getProductsMedicine() {
        try {
            List<Product> products = medicalRecordService.getProductsMedicine();
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (Exception e) {
            System.err.println("Error in getProductsMedicine: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi khi lấy danh sách thuốc: " + e.getMessage()));
        }
    }

    @GetMapping("/appointments/{appointmentId}/status")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getAppointmentStatus(@PathVariable Integer appointmentId) {
        try {
            Appointment appointment = appointmentService.getAppointmentById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Cuộc hẹn không tồn tại!"));
            return ResponseEntity.ok(new StatusResponse(appointment.getStatus().name()));
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getAppointmentStatus: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getAppointmentStatus: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy trạng thái cuộc hẹn: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/doctor/{doctorId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDoctorMedicalRecords(@PathVariable Integer doctorId) {
        try {
            List<MedicalRecord> records = medicalRecordService.getDoctorMedicalRecordsByUserId(doctorId);
            return ResponseEntity.ok(records);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getDoctorMedicalRecords: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getDoctorMedicalRecords: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy hồ sơ bệnh án bác sĩ: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/patient/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public ResponseEntity<?> getPatientMedicalRecords(@PathVariable Integer patientId) {
        try {
            List<MedicalRecord> records = medicalRecordService.getPatientMedicalRecordsByUserId(patientId);
            return ResponseEntity.ok(records);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getPatientMedicalRecords: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getPatientMedicalRecords: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy hồ sơ bệnh án bệnh nhân: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/{recordId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getMedicalRecordById(@PathVariable Integer recordId) {
        try {
            MedicalRecord record = medicalRecordService.getMedicalRecordById(recordId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ điều trị với ID: " + recordId));
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getMedicalRecordById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getMedicalRecordById: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy hồ sơ bệnh án: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/by-appointment/{appointmentId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> checkMedicalRecordExistsByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            boolean exists = medicalRecordService.existsByAppointmentId(appointmentId);
            return ResponseEntity.ok(new ExistsResponse(exists));
        } catch (Exception e) {
            System.err.println("Error in checkMedicalRecordExistsByAppointmentId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi kiểm tra hồ sơ bệnh án: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/by-appointment/{appointmentId}/record")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getMedicalRecordByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            MedicalRecord record = medicalRecordService.getMedicalRecordByAppointmentId(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ y tế cho cuộc hẹn này"));
            return ResponseEntity.ok(record);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getMedicalRecordByAppointmentId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getMedicalRecordByAppointmentId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy hồ sơ bệnh án theo cuộc hẹn: " + e.getMessage()));
        }
    }

    @GetMapping("/medical-records/by-appointment/{appointmentId}/medications")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PATIENT')")
    public ResponseEntity<?> getMedicationsByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            if (!medicalRecordService.existsByAppointmentId(appointmentId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Không tìm thấy hồ sơ y tế cho cuộc hẹn này"));
            }

            MedicalRecord medicalRecord = medicalRecordService.getMedicalRecordByAppointmentId(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ y tế cho cuộc hẹn này"));

            double totalMedicationAmount = medicalRecord.getRecommendedProducts().stream()
                    .filter(mrp -> mrp.getProduct().getType() == org.eyespire.eyespireapi.model.enums.ProductType.MEDICINE)
                    .mapToDouble(mrp -> mrp.getQuantity() * mrp.getProduct().getPrice().doubleValue())
                    .sum();

            Map<String, Object> response = Map.of(
                    "products", medicalRecord.getRecommendedProducts().stream()
                            .filter(mrp -> mrp.getProduct().getType() == org.eyespire.eyespireapi.model.enums.ProductType.MEDICINE)
                            .map(mrp -> Map.of(
                                    "id", mrp.getProduct().getId(),
                                    "name", mrp.getProduct().getName(),
                                    "price", mrp.getProduct().getPrice(),
                                    "quantity", mrp.getQuantity(),
                                    "totalPrice", mrp.getQuantity() * mrp.getProduct().getPrice().doubleValue()
                            ))
                            .collect(Collectors.toList()),
                    "totalAmount", totalMedicationAmount
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in getMedicationsByAppointmentId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in getMedicationsByAppointmentId: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi lấy thông tin thuốc: " + e.getMessage()));
        }
    }

    @PutMapping("/medical-records/{recordId}")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> updateMedicalRecord(
            @PathVariable Integer recordId,
            @RequestParam(value = "diagnosis", required = false) String diagnosis,
            @RequestParam(value = "serviceId", required = false) Integer serviceId,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "productQuantities", required = false) String productQuantitiesJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "filesToDelete", required = false) String filesToDeleteJson) {
        try {
            System.out.println("Received PUT /medical-records/" + recordId + ":");
            System.out.println("Diagnosis: " + diagnosis);
            System.out.println("ServiceId: " + serviceId);
            System.out.println("Notes: " + notes);
            System.out.println("ProductQuantitiesJson: " + productQuantitiesJson);
            System.out.println("Files: " + (files != null ? Arrays.stream(files).map(MultipartFile::getOriginalFilename).collect(Collectors.toList()) : "none"));
            System.out.println("FilesToDeleteJson: " + filesToDeleteJson);

            if (recordId == null || recordId <= 0) {
                return ResponseEntity.badRequest().body(new ErrorResponse("ID hồ sơ không hợp lệ!"));
            }

            List<Map<String, Integer>> productQuantities = null;
            if (productQuantitiesJson != null && !productQuantitiesJson.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    productQuantities = mapper.readValue(productQuantitiesJson,
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                    for (Map<String, Integer> pq : productQuantities) {
                        if (!pq.containsKey("productId") || !pq.containsKey("quantity") ||
                                pq.get("productId") == null || pq.get("productId") <= 0 ||
                                pq.get("quantity") == null || pq.get("quantity") <= 0) {
                            return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không hợp lệ: " + pq));
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing productQuantitiesJson: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu productQuantities không đúng định dạng JSON: " + e.getMessage()));
                }
            }

            List<String> filesToDelete = null;
            if (filesToDeleteJson != null && !filesToDeleteJson.isEmpty()) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    filesToDelete = mapper.readValue(filesToDeleteJson,
                            mapper.getTypeFactory().constructCollectionType(List.class, String.class));
                } catch (Exception e) {
                    System.err.println("Error parsing filesToDeleteJson: " + e.getMessage());
                    e.printStackTrace();
                    return ResponseEntity.badRequest().body(new ErrorResponse("Dữ liệu filesToDelete không đúng định dạng JSON: " + e.getMessage()));
                }
            }

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

            MedicalRecord updatedRecord = medicalRecordService.updateMedicalRecord(recordId, diagnosis, notes, productQuantities, files, filesToDelete);
            if (serviceId != null && updatedRecord.getAppointment() != null) {
                appointmentService.updateAppointmentService(updatedRecord.getAppointment().getId(), serviceId);
            }
            return new ResponseEntity<>(updatedRecord, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            System.err.println("IllegalArgumentException in updateMedicalRecord: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error in updateMedicalRecord: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Lỗi server khi cập nhật hồ sơ bệnh án: " + e.getMessage()));
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