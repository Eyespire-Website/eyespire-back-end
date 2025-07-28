package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTransactionNo(String transactionNo);

    /**
     * Tìm tất cả các thanh toán của một người dùng
     * @param userId ID của người dùng
     * @return Danh sách các thanh toán
     */
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId ORDER BY p.createdAt DESC")
    List<Payment> findByUserId(@Param("userId") Integer userId);
    
    /**
     * Tìm tất cả các thanh toán liên quan đến một lịch hẹn
     * @param appointmentId ID của lịch hẹn
     * @return Danh sách các thanh toán
     */
    @Query("SELECT p FROM Payment p WHERE p.appointmentInvoice.appointment.id = :appointmentId ORDER BY p.createdAt DESC")
    List<Payment> findByAppointmentId(@Param("appointmentId") Integer appointmentId);
    
    /**
     * Tìm tất cả các thanh toán theo trạng thái
     * @param userId ID của người dùng
     * @param status Trạng thái thanh toán
     * @return Danh sách các thanh toán
     */
    @Query("SELECT p FROM Payment p WHERE p.userId = :userId AND p.status = :status ORDER BY p.createdAt DESC")
    List<Payment> findByUserIdAndStatus(@Param("userId") Integer userId, @Param("status") String status);
}