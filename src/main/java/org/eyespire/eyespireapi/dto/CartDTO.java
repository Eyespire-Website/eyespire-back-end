package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Integer id;
    private Integer userId;
    private String userName;
    private List<CartItemDTO> items = new ArrayList<>();
    private Integer totalItems = 0;
    private Double totalPrice = 0.0;
}
