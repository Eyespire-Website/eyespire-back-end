package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDTO {
    private String id;
    private String date;
    private String serviceName;
    private String amount;
    private String status;
    private String type; // "service" hoặc "order"
    private String transactionNo;
    private String paymentMethod;
    private String payerName;
    private String paymentTime;
    private String notes;
    private Boolean isExpanded;
    
    // Thông tin bổ sung cho chi tiết
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Thông tin cho hóa đơn dịch vụ
    private Integer appointmentId;
    private String doctorName;
    private String appointmentDate;
    private String timeSlot;
    
    // Thông tin cho hóa đơn đơn hàng
    private Integer orderId;
    private String shippingAddress;
}
