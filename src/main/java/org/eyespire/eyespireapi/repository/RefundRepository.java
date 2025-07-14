package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Refund;
import org.eyespire.eyespireapi.model.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Integer> {

    /**
     * Tìm tất cả hoàn tiền đang chờ xử lý
     */
    List<Refund> findByRefundStatusOrderByCreatedAtDesc(RefundStatus refundStatus);

    /**
     * Tìm hoàn tiền theo patient ID
     */
    List<Refund> findByPatientIdOrderByCreatedAtDesc(Integer patientId);

    /**
     * Tìm hoàn tiền theo appointment ID
     */
    List<Refund> findByAppointmentId(Integer appointmentId);

    /**
     * Tìm hoàn tiền theo appointment ID sắp xếp theo thời gian
     */
    List<Refund> findByAppointmentIdOrderByCreatedAtDesc(Integer appointmentId);

    /**
     * Thống kê hoàn tiền theo khoảng thời gian
     */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    Long countRefundsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Tổng số tiền hoàn theo khoảng thời gian
     */
    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Refund r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumRefundAmountByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);

    /**
     * Đếm hoàn tiền theo trạng thái và khoảng thời gian
     */
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.refundStatus = :status AND r.createdAt BETWEEN :startDate AND :endDate")
    Long countRefundsByStatusAndDateRange(@Param("status") RefundStatus status,
                                         @Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate);

    /**
     * Thống kê hoàn tiền theo phương thức
     */
    @Query("SELECT r.refundMethod, COUNT(r) FROM Refund r WHERE r.refundStatus = 'COMPLETED' AND r.createdAt BETWEEN :startDate AND :endDate GROUP BY r.refundMethod")
    List<Object[]> countRefundsByMethodAndDateRange(@Param("startDate") LocalDateTime startDate, 
                                                   @Param("endDate") LocalDateTime endDate);
}
