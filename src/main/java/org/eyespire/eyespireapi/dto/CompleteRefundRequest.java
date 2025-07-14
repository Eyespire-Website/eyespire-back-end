package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.RefundMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteRefundRequest {
    private RefundMethod refundMethod;
    private String notes;
    
    @Override
    public String toString() {
        return "CompleteRefundRequest{" +
                "refundMethod=" + refundMethod +
                ", notes='" + notes + '\'' +
                '}';
    }
}
