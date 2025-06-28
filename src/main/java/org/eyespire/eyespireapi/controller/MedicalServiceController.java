package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.model.MedicalService;
import org.eyespire.eyespireapi.service.MedicalServiceService;
import org.eyespire.eyespireapi.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/medical-services")
@CrossOrigin(origins = "*")
public class MedicalServiceController {
    
    @Autowired
    private MedicalServiceService medicalServiceService;

    @Autowired
    private FileStorageService fileStorageService;
    
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
     * Lấy danh sách dịch vụ y tế của bác sĩ
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<MedicalService>> getMedicalServicesByDoctor(@PathVariable Integer doctorId) {
        return ResponseEntity.ok(medicalServiceService.getMedicalServicesByDoctor(doctorId));
    }
    
    /**
     * Tìm kiếm dịch vụ y tế theo tên
     */
    @GetMapping("/search")
    public ResponseEntity<List<MedicalService>> searchMedicalServices(@RequestParam String keyword) {
        return ResponseEntity.ok(medicalServiceService.searchMedicalServices(keyword));
    }

    /**
     * Tạo mới dịch vụ y tế
     */
    @PostMapping
    public ResponseEntity<MedicalService> createMedicalService(@RequestBody MedicalService medicalService) {
        MedicalService createdService = medicalServiceService.createMedicalService(medicalService);
        return ResponseEntity.ok(createdService);
    }

    /**
     * Cập nhật thông tin dịch vụ y tế
     */
    @PutMapping("/{id}")
    public ResponseEntity<MedicalService> updateMedicalService(
            @PathVariable Integer id,
            @RequestBody MedicalService medicalServiceDetails) {
        
        MedicalService updatedService = medicalServiceService.updateMedicalService(id, medicalServiceDetails);
        if (updatedService == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(updatedService);
    }

    /**
     * Xóa dịch vụ y tế
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalService(@PathVariable Integer id) {
        boolean deleted = medicalServiceService.deleteMedicalService(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().build();
    }

    
    /**
     * API upload ảnh cho dịch vụ y tế
     * @param image File ảnh cần upload
     * @return URL của ảnh đã upload
     */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadServiceImage(
            @RequestParam("image") MultipartFile image) {
        
        // Kiểm tra file
        if (image.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể upload file trống");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Kiểm tra định dạng file
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Chỉ chấp nhận file ảnh");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Lưu file vào thư mục gốc (không sử dụng thư mục con)
            String imageUrl = fileStorageService.storeImage(image, "");
            
            // Trả về URL của ảnh
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", imageUrl);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Không thể lưu file: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
