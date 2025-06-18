package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreateRequest {
    private BigDecimal amount;
    private AppointmentDTO appointmentData;
    private String returnUrl;
}
