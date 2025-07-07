package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSVerifyResponse {
    private boolean success;
    private String message;
    private String status;
    private Integer paymentId;
    private String transactionNo;
    private String orderCode;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String payosTransactionId;
    private AppointmentDTO appointmentData;
    private Map<String, Object> orderData;
}
