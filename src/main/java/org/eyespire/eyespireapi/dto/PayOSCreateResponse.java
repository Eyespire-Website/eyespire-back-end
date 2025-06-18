package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreateResponse {
    private Integer paymentId;
    private String paymentUrl;
    private String transactionNo;
}
