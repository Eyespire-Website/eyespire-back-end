package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Order;
import org.eyespire.eyespireapi.model.OrderPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
