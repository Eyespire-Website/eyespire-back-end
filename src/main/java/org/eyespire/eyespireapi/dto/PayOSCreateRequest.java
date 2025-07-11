package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreateRequest {
    private BigDecimal amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private Map<String, Object> orderData;
    private Integer orderId;
    private AppointmentDTO appointmentData;
}