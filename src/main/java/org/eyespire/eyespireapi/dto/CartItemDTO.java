package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productImage;
    private Integer quantity;
    private Double price;
    private Double totalPrice;
}
