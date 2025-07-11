package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPayOSCreateResponse {
    private Integer paymentId;
    private String checkoutUrl;
    private String transactionNo;
    private boolean success;
    private String message;
    private String payosTransactionId;
}
