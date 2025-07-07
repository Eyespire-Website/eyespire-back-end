package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPayOSCreateRequest {
    private BigDecimal amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private Map<String, Object> orderData;
    private Integer orderId;
    private AppointmentDTO appointmentData;
    private String orderCode;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private Map<String, String> metadata;
}
