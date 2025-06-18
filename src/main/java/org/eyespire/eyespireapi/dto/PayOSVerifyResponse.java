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
public class PayOSVerifyResponse {
    private boolean success;
    private String message;
    private Integer paymentId;
    private String transactionNo;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private AppointmentDTO appointmentData;
}
