package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Order;
import org.eyespire.eyespireapi.model.OrderPayment;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Integer> {
    /**
     * Tìm thanh toán theo mã giao dịch
     * @param transactionNo Mã giao dịch
     * @return Thông tin thanh toán
     */
    Optional<OrderPayment> findByTransactionNo(String transactionNo);
    
    /**
     * Tìm thanh toán mới nhất của đơn hàng
     * @param order Đơn hàng cần tìm thanh toán
     * @return Thanh toán mới nhất
     */
    Optional<OrderPayment> findTopByOrderOrderByIdDesc(Order order);
    
    /**
     * Tìm tất cả các thanh toán của đơn hàng của một người dùng
     * @param patientId ID của người dùng (bệnh nhân)
     * @return Danh sách các thanh toán đơn hàng
     */
    @Query("SELECT op FROM OrderPayment op JOIN op.order o WHERE o.patient.id = :patientId ORDER BY op.createdAt DESC")
    List<OrderPayment> findByOrderPatientId(@Param("patientId") Integer patientId);
    
    /**
     * Tìm tất cả các thanh toán đơn hàng theo trạng thái
     * @param patientId ID của người dùng (bệnh nhân)
     * @param status Trạng thái thanh toán
     * @return Danh sách các thanh toán đơn hàng
     */
    @Query("SELECT op FROM OrderPayment op JOIN op.order o WHERE o.patient.id = :patientId AND op.status = :status ORDER BY op.createdAt DESC")
    List<OrderPayment> findByOrderPatientIdAndStatus(@Param("patientId") Integer patientId, @Param("status") PaymentStatus status);
    
    /**
     * Tìm tất cả các thanh toán của một đơn hàng
     * @param orderId ID của đơn hàng
     * @return Danh sách các thanh toán
     */
    @Query("SELECT op FROM OrderPayment op WHERE op.order.id = :orderId ORDER BY op.createdAt DESC")
    List<OrderPayment> findByOrderId(@Param("orderId") Integer orderId);
}
