package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Integer id;
    private Long userId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDate orderDate;
    private String shippingAddress;
    private LocalDateTime createdAt;
    private List<OrderItemDTO> items;
    private PaymentDTO payment;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Integer id;
        private Integer productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDTO {
        private Integer id;
        private String transactionNo;
        private BigDecimal amount;
        private String status;
        private LocalDateTime paymentDate;
    }
}
