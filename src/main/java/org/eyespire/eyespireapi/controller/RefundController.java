package org.eyespire.eyespireapi.controller;

import lombok.RequiredArgsConstructor;
import org.eyespire.eyespireapi.dto.CompleteRefundRequest;
import org.eyespire.eyespireapi.dto.RefundDTO;
import org.eyespire.eyespireapi.dto.RefundStatsDTO;
import org.eyespire.eyespireapi.model.Refund;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.service.RefundService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RefundController {

    private final RefundService refundService;

    /**
     * Lấy danh sách hoàn tiền đang chờ xử lý
     * GET /api/refunds/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<List<RefundDTO>> getPendingRefunds() {
        try {
            List<Refund> refunds = refundService.getPendingRefunds();
            List<RefundDTO> refundDTOs = refunds.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(refundDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Đánh dấu hoàn tiền hoàn tất
     * PUT /api/refunds/{id}/complete
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<RefundDTO> completeRefund(
            @PathVariable Integer id,
            @RequestBody CompleteRefundRequest request,
            @RequestHeader("X-User-Name") String userName,
            @RequestHeader("X-User-Role") String userRole) {
        try {
            System.out.println("=== CompleteRefund Debug ===");
            System.out.println("Refund ID: " + id);
            System.out.println("User Name: " + userName);
            System.out.println("User Role: " + userRole);
            System.out.println("Request: " + request);
            System.out.println("Refund Method: " + request.getRefundMethod());
            System.out.println("Notes: " + request.getNotes());
            
            UserRole role = UserRole.valueOf(userRole.toUpperCase());
            Refund refund = refundService.completeRefund(
                    id, 
                    request.getRefundMethod(), 
                    userName, 
                    role, 
                    request.getNotes()
            );
            System.out.println("Refund completed successfully: " + refund.getId());
            return ResponseEntity.ok(convertToDTO(refund));
        } catch (RuntimeException e) {
            System.err.println("RuntimeException in completeRefund: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            System.err.println("Exception in completeRefund: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy lịch sử hoàn tiền của một bệnh nhân
     * GET /api/refunds/user/{userId}
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<List<RefundDTO>> getUserRefundHistory(@PathVariable Integer userId) {
        try {
            List<Refund> refunds = refundService.getUserRefundHistory(userId);
            List<RefundDTO> refundDTOs = refunds.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(refundDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Thống kê hoàn tiền
     * GET /api/refunds/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefundStatsDTO> getRefundStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        try {
            // Mặc định là 30 ngày gần nhất nếu không có tham số
            if (startDate == null) {
                startDate = LocalDateTime.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }

            Map<String, Object> stats = refundService.getRefundStats(startDate, endDate);
            
            RefundStatsDTO statsDTO = new RefundStatsDTO();
            statsDTO.setTotalRefunds((Long) stats.get("totalRefunds"));
            statsDTO.setTotalAmount((java.math.BigDecimal) stats.get("totalAmount"));
            statsDTO.setPendingRefunds((Long) stats.get("pendingRefunds"));
            statsDTO.setCompletedRefunds((Long) stats.get("completedRefunds"));
            statsDTO.setMethodBreakdown((Map<String, Long>) stats.get("methodBreakdown"));
            
            return ResponseEntity.ok(statsDTO);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy tất cả hoàn tiền (cho admin)
     * GET /api/refunds
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RefundDTO>> getAllRefunds() {
        try {
            List<Refund> refunds = refundService.getAllRefunds();
            List<RefundDTO> refundDTOs = refunds.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(refundDTOs);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }



    /**
     * Lấy trạng thái hoàn tiền của cuộc hẹn
     * GET /api/refunds/appointment/{appointmentId}
     */
    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST') or hasRole('PATIENT')")
    public ResponseEntity<RefundDTO> getRefundByAppointmentId(@PathVariable Integer appointmentId) {
        try {
            List<Refund> refunds = refundService.getRefundByAppointmentId(appointmentId);
            if (refunds.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            // Lấy refund mới nhất nếu có nhiều
            Refund latestRefund = refunds.get(0);
            return ResponseEntity.ok(convertToDTO(latestRefund));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Lấy chi tiết một hoàn tiền
     * GET /api/refunds/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<RefundDTO> getRefundById(@PathVariable Integer id) {
        try {
            return refundService.getRefundById(id)
                    .map(refund -> ResponseEntity.ok(convertToDTO(refund)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Convert Refund entity to DTO
     */
    private RefundDTO convertToDTO(Refund refund) {
        RefundDTO dto = new RefundDTO();
        dto.setId(refund.getId());
        dto.setAppointmentId(refund.getAppointment().getId());
        dto.setPatientId(refund.getPatient().getId());
        dto.setPatientName(refund.getAppointment().getPatientName());
        dto.setPatientEmail(refund.getAppointment().getPatientEmail());
        dto.setPatientPhone(refund.getAppointment().getPatientPhone());
        dto.setAppointmentTime(refund.getAppointment().getAppointmentTime());
        dto.setRefundAmount(refund.getRefundAmount());
        dto.setRefundReason(refund.getRefundReason());
        dto.setRefundStatus(refund.getRefundStatus());
        dto.setRefundMethod(refund.getRefundMethod());
        dto.setRefundCompletedBy(refund.getRefundCompletedBy());
        dto.setRefundCompletedByRole(refund.getRefundCompletedByRole());
        dto.setRefundCompletedAt(refund.getRefundCompletedAt());
        dto.setNotes(refund.getNotes());
        dto.setCreatedAt(refund.getCreatedAt());
        dto.setUpdatedAt(refund.getUpdatedAt());
        return dto;
    }
    
    /**
     * Tạo refund cho các appointment đã hủy nhưng chưa có refund
     * POST /api/refunds/create-missing
     */
    @PostMapping("/create-missing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createMissingRefunds() {
        try {
            // Lấy tất cả appointment đã hủy
            List<org.eyespire.eyespireapi.model.Appointment> cancelledAppointments = 
                refundService.getCancelledAppointmentsWithoutRefund();
            
            int created = 0;
            int failed = 0;
            
            for (org.eyespire.eyespireapi.model.Appointment appointment : cancelledAppointments) {
                try {
                    refundService.createRefundForCanceledAppointment(
                        appointment, 
                        appointment.getCancellationReason() != null ? 
                            appointment.getCancellationReason() : "Hủy cuộc hẹn"
                    );
                    created++;
                } catch (Exception e) {
                    System.err.println("Lỗi tạo refund cho appointment " + appointment.getId() + ": " + e.getMessage());
                    failed++;
                }
            }
            
            Map<String, Object> result = Map.of(
                "message", "Hoàn thành tạo refund cho các appointment đã hủy",
                "created", created,
                "failed", failed,
                "total", cancelledAppointments.size()
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of("error", "Lỗi khi tạo refund: " + e.getMessage()));
        }
    }
}
