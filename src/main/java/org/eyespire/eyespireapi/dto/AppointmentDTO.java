package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.eyespire.eyespireapi.model.MedicalService;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDTO {
    private Integer id;
    private Integer userId;         // ID của người dùng đã đăng nhập (nếu có)
    private Integer doctorId;       // ID của bác sĩ được chọn
    private Integer serviceId;      // ID của dịch vụ được chọn
    private String appointmentDate; // Ngày khám (định dạng yyyy-MM-dd)
    private String timeSlot;        // Khung giờ khám (định dạng HH:mm)
    private String patientName;     // Tên bệnh nhân
    private String patientEmail;    // Email của bệnh nhân
    private String patientPhone;    // Số điện thoại của bệnh nhân
    private String notes;           // Ghi chú bổ sung
    private Integer paymentId;      // ID của thanh toán liên kết với lịch hẹn
    private String status;
    private UserDTO patient;
    
    // Thông tin thanh toán
    private BigDecimal totalAmount;      // Tổng chi phí
    private BigDecimal depositAmount;    // Tiền đặt cọc
    private BigDecimal remainingAmount;  // Số tiền còn lại
    private Boolean isFullyPaid;         // Đã thanh toán đầy đủ chưa
    private LocalDateTime paidAt;        // Thời gian thanh toán
    
    // Thông tin dịch vụ và bác sĩ
    private MedicalService service;      // Đối tượng dịch vụ
    private DoctorDTO doctor;            // Đối tượng bác sĩ
}
