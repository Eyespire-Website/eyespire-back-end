package org.eyespire.eyespireapi.repository;

import org.eyespire.eyespireapi.model.AppointmentInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentInvoiceRepository extends JpaRepository<AppointmentInvoice, Integer>, AppointmentInvoiceRepositoryCustom {
    
    // Tìm hóa đơn theo ID cuộc hẹn
    Optional<AppointmentInvoice> findByAppointmentId(Integer appointmentId);
    
    // Tìm các hóa đơn chưa thanh toán đầy đủ
    List<AppointmentInvoice> findByIsFullyPaidFalse();
    
    // Tìm các hóa đơn đã thanh toán đầy đủ
    List<AppointmentInvoice> findByIsFullyPaidTrue();
    
    /**
     * Tìm tất cả hóa đơn cuộc hẹn của bệnh nhân với trạng thái cuộc hẹn COMPLETED
     * @param patientId ID của bệnh nhân
     * @return Danh sách hóa đơn cuộc hẹn đã hoàn thành
     */
    @Query("SELECT ai FROM AppointmentInvoice ai JOIN ai.appointment a WHERE a.patient.id = :patientId AND a.status = 'COMPLETED' ORDER BY ai.createdAt DESC")
    List<AppointmentInvoice> findByPatientIdAndAppointmentCompleted(@Param("patientId") Integer patientId);
}
