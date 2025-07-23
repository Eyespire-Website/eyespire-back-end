package org.eyespire.eyespireapi.service;

import lombok.RequiredArgsConstructor;
import org.eyespire.eyespireapi.model.Appointment;
import org.eyespire.eyespireapi.model.Refund;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;
import org.eyespire.eyespireapi.model.enums.RefundMethod;
import org.eyespire.eyespireapi.model.enums.RefundStatus;
import org.eyespire.eyespireapi.model.enums.UserRole;
import org.eyespire.eyespireapi.repository.AppointmentRepository;
import org.eyespire.eyespireapi.repository.RefundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Tạo yêu cầu hoàn tiền khi hủy cuộc hẹn
     */
    @Transactional
    public Refund createRefund(Integer appointmentId, String reason) {
        Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
        if (appointmentOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy cuộc hẹn");
        }

        Appointment appointment = appointmentOpt.get();
        
        // Kiểm tra xem đã có refund cho appointment này chưa
        List<Refund> existingRefunds = refundRepository.findByAppointmentId(appointmentId);
        if (!existingRefunds.isEmpty()) {
            throw new RuntimeException("Cuộc hẹn này đã có yêu cầu hoàn tiền");
        }

        // Tạo refund mới
        Refund refund = new Refund();
        refund.setAppointment(appointment);
        refund.setPatient(appointment.getPatient());
        refund.setRefundAmount(new BigDecimal("10000")); // Tiền cọc 10,000 VNĐ
        refund.setRefundReason(reason);
        refund.setRefundStatus(RefundStatus.PENDING_MANUAL_REFUND);

        return refundRepository.save(refund);
    }

    /**
     * Tạo refund cho cuộc hẹn đã hủy (wrapper method cho AppointmentService)
     */
    @Transactional
    public Refund createRefundForCanceledAppointment(Appointment appointment, String reason) {
        return createRefund(appointment.getId(), reason);
    }

    /**
     * Lấy danh sách hoàn tiền đang chờ xử lý
     */
    public List<Refund> getPendingRefunds() {
        return refundRepository.findByRefundStatusOrderByCreatedAtDesc(RefundStatus.PENDING_MANUAL_REFUND);
    }

    /**
     * Đánh dấu hoàn tiền hoàn tất
     */
    @Transactional
    public Refund completeRefund(Integer refundId, RefundMethod method, String completedBy, UserRole completedByRole, String notes) {
        Optional<Refund> refundOpt = refundRepository.findById(refundId);
        if (refundOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy yêu cầu hoàn tiền");
        }

        Refund refund = refundOpt.get();
        if (refund.getRefundStatus() != RefundStatus.PENDING_MANUAL_REFUND) {
            throw new RuntimeException("Yêu cầu hoàn tiền này đã được xử lý");
        }

        // Cập nhật refund
        refund.setRefundStatus(RefundStatus.COMPLETED);
        refund.setRefundMethod(method);
        refund.setRefundCompletedBy(completedBy);
        refund.setRefundCompletedByRole(completedByRole);
        refund.setRefundCompletedAt(LocalDateTime.now());
        refund.setNotes(notes);

        return refundRepository.save(refund);
    }

    /**
     * Lấy lịch sử hoàn tiền của một bệnh nhân
     */
    public List<Refund> getUserRefundHistory(Integer userId) {
        return refundRepository.findByPatientIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Thống kê hoàn tiền
     */
    public Map<String, Object> getRefundStats(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();
        
        // Tổng số hoàn tiền
        Long totalRefunds = refundRepository.countRefundsByDateRange(startDate, endDate);
        stats.put("totalRefunds", totalRefunds);
        
        // Tổng số tiền hoàn
        BigDecimal totalAmount = refundRepository.sumRefundAmountByDateRange(startDate, endDate);
        stats.put("totalAmount", totalAmount);
        
        // Số hoàn tiền đang chờ
        Long pendingRefunds = refundRepository.countRefundsByStatusAndDateRange(
            RefundStatus.PENDING_MANUAL_REFUND, startDate, endDate);
        stats.put("pendingRefunds", pendingRefunds);
        
        // Số hoàn tiền đã hoàn tất
        Long completedRefunds = refundRepository.countRefundsByStatusAndDateRange(
            RefundStatus.COMPLETED, startDate, endDate);
        stats.put("completedRefunds", completedRefunds);
        
        // Thống kê theo phương thức hoàn tiền
        List<Object[]> methodStats = refundRepository.countRefundsByMethodAndDateRange(startDate, endDate);
        Map<String, Long> methodBreakdown = new HashMap<>();
        for (Object[] stat : methodStats) {
            RefundMethod method = (RefundMethod) stat[0];
            Long count = (Long) stat[1];
            methodBreakdown.put(method != null ? method.name() : "UNKNOWN", count);
        }
        stats.put("methodBreakdown", methodBreakdown);
        
        return stats;
    }

    /**
     * Lấy refund theo ID
     */
    public Optional<Refund> getRefundById(Integer id) {
        return refundRepository.findById(id);
    }

    /**
     * Lấy refund theo appointment ID
     */
    public List<Refund> getRefundByAppointmentId(Integer appointmentId) {
        return refundRepository.findByAppointmentIdOrderByCreatedAtDesc(appointmentId);
    }

    /**
     * Lấy tất cả refunds
     */
    public List<Refund> getAllRefunds() {
        return refundRepository.findAll();
    }
    
    /**
     * Lấy danh sách bệnh nhân có lịch sử hoàn tiền
     */
    public List<Map<String, Object>> getPatientsWithRefunds() {
        List<Refund> allRefunds = refundRepository.findAll();
        
        // Group refunds by patient
        Map<Integer, Map<String, Object>> patientMap = new HashMap<>();
        
        for (Refund refund : allRefunds) {
            Integer patientId = refund.getPatient().getId();
            
            if (!patientMap.containsKey(patientId)) {
                Map<String, Object> patientInfo = new HashMap<>();
                patientInfo.put("id", patientId);
                patientInfo.put("name", refund.getAppointment().getPatientName());
                patientInfo.put("email", refund.getAppointment().getPatientEmail());
                patientInfo.put("phone", refund.getAppointment().getPatientPhone());
                patientInfo.put("totalRefunds", 0);
                patientInfo.put("totalRefundAmount", BigDecimal.ZERO);
                patientMap.put(patientId, patientInfo);
            }
            
            Map<String, Object> patientInfo = patientMap.get(patientId);
            patientInfo.put("totalRefunds", (Integer) patientInfo.get("totalRefunds") + 1);
            
            BigDecimal currentAmount = (BigDecimal) patientInfo.get("totalRefundAmount");
            BigDecimal refundAmount = refund.getRefundAmount() != null ? refund.getRefundAmount() : BigDecimal.ZERO;
            patientInfo.put("totalRefundAmount", currentAmount.add(refundAmount));
        }
        
        return patientMap.values().stream()
                .sorted((a, b) -> ((String) a.get("name")).compareToIgnoreCase((String) b.get("name")))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * Lấy các appointment đã hủy nhưng chưa có refund
     */
    public List<Appointment> getCancelledAppointmentsWithoutRefund() {
        // Lấy tất cả appointment đã hủy
        List<Appointment> cancelledAppointments = appointmentRepository.findByStatus(
            AppointmentStatus.CANCELED
        );
        
        // Lọc ra những appointment chưa có refund
        return cancelledAppointments.stream()
            .filter(appointment -> {
                List<Refund> existingRefunds = refundRepository.findByAppointmentId(appointment.getId());
                return existingRefunds.isEmpty();
            })
            .collect(java.util.stream.Collectors.toList());
    }
}
