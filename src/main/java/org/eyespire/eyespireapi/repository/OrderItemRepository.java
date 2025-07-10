package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    
    /**
     * Tìm danh sách các mục trong đơn hàng
     * @param orderId ID của đơn hàng
     * @return Danh sách các mục trong đơn hàng
     */
    List<OrderItem> findByOrderId(Integer orderId);
    @Query("SELECT oi.product.id AS productId, SUM(oi.quantity) AS totalQuantity " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY oi.product.id")
    List<Map<String, Object>> findTotalSalesByProduct();
}
