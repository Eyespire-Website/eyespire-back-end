package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentInvoiceRepository extends JpaRepository<AppointmentInvoice, Integer> {
    
    // Tìm hóa đơn theo ID cuộc hẹn
    Optional<AppointmentInvoice> findByAppointmentId(Integer appointmentId);
    
    // Tìm các hóa đơn chưa thanh toán đầy đủ
    java.util.List<AppointmentInvoice> findByIsFullyPaidFalse();
    
    // Tìm các hóa đơn đã thanh toán đầy đủ
    java.util.List<AppointmentInvoice> findByIsFullyPaidTrue();
}
