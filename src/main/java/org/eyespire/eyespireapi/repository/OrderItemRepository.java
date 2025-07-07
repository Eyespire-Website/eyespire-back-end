package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    
    /**
     * Tìm danh sách các mục trong đơn hàng
     * @param orderId ID của đơn hàng
     * @return Danh sách các mục trong đơn hàng
     */
    List<OrderItem> findByOrderId(Integer orderId);
}
