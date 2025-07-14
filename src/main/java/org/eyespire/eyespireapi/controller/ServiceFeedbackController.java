package org.eyespire.eyespireapi.controller;

import org.eyespire.eyespireapi.dto.ServiceFeedbackDTO;
import org.eyespire.eyespireapi.model.ServiceFeedback;
import org.eyespire.eyespireapi.service.ServiceFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-feedback")
@CrossOrigin(origins = "*")
public class ServiceFeedbackController {

    @Autowired
    private ServiceFeedbackService serviceFeedbackService;

    /**
     * Tạo feedback mới cho appointment
     */
    @PostMapping
    public ResponseEntity<?> createFeedback(@RequestBody ServiceFeedbackDTO feedbackDTO) {
        try {
            ServiceFeedback feedback = serviceFeedbackService.createFeedback(feedbackDTO);
            return ResponseEntity.ok(feedback);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy feedback theo appointment ID
     */
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getFeedbackByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            ServiceFeedback feedback = serviceFeedbackService.getFeedbackByAppointmentId(appointmentId);
            if (feedback != null) {
                return ResponseEntity.ok(feedback);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy feedback theo patient ID
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getFeedbacksByPatientId(@PathVariable Integer patientId) {
        try {
            List<ServiceFeedback> feedbacks = serviceFeedbackService.getFeedbacksByPatientId(patientId);
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Cập nhật feedback
     */
    @PutMapping("/{feedbackId}")
    public ResponseEntity<?> updateFeedback(@PathVariable Integer feedbackId, 
                                          @RequestBody ServiceFeedbackDTO feedbackDTO) {
        try {
            ServiceFeedback updatedFeedback = serviceFeedbackService.updateFeedback(feedbackId, feedbackDTO);
            return ResponseEntity.ok(updatedFeedback);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Xóa feedback
     */
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<?> deleteFeedback(@PathVariable Integer feedbackId) {
        try {
            serviceFeedbackService.deleteFeedback(feedbackId);
            return ResponseEntity.ok(Map.of("message", "Feedback deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy tất cả feedback (cho admin)
     */
    @GetMapping
    public ResponseEntity<?> getAllFeedbacks() {
        try {
            List<ServiceFeedback> feedbacks = serviceFeedbackService.getAllFeedbacks();
            return ResponseEntity.ok(feedbacks);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lấy thống kê rating
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getRatingStatistics() {
        try {
            Map<String, Object> statistics = serviceFeedbackService.getRatingStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Kiểm tra xem appointment đã có feedback chưa
     */
    @GetMapping("/exists/appointment/{appointmentId}")
    public ResponseEntity<?> checkFeedbackExists(@PathVariable Integer appointmentId) {
        try {
            boolean exists = serviceFeedbackService.feedbackExistsForAppointment(appointmentId);
            return ResponseEntity.ok(Map.of("exists", exists));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}