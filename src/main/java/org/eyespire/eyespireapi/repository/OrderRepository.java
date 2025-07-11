package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    /**
     * Tìm danh sách đơn hàng của người dùng, sắp xếp theo thời gian tạo giảm dần
     * @param patientId ID của người dùng
     * @return Danh sách đơn hàng
     */
    List<Order> findByPatientIdOrderByCreatedAtDesc(Long patientId);

}