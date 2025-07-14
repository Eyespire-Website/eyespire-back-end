package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.RefundMethod;
import org.eyespire.eyespireapi.model.enums.RefundStatus;
import org.eyespire.eyespireapi.model.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundDTO {
    private Integer id;
    private Integer appointmentId;
    private Integer patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private LocalDateTime appointmentTime;
    private BigDecimal refundAmount;
    private String refundReason;
    private RefundStatus refundStatus;
    private RefundMethod refundMethod;
    private String refundCompletedBy;
    private UserRole refundCompletedByRole;
    private LocalDateTime refundCompletedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
