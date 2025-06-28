package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.Product;
import org.eyespire.eyespireapi.model.enums.ProductType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ProductType type;
    private String status; // Trạng thái hiển thị: "Còn hàng", "Sắp hết", "Hết hàng"
    
    public static ProductDTO fromEntity(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setCreatedAt(product.getCreatedAt());
        dto.setUpdatedAt(product.getUpdatedAt());
        dto.setType(product.getType());
        
        // Xác định trạng thái dựa trên số lượng tồn kho
        if (product.getStockQuantity() <= 0) {
            dto.setStatus("Hết hàng");
        } else if (product.getStockQuantity() <= 5) {
            dto.setStatus("Sắp hết");
        } else {
            dto.setStatus("Còn hàng");
        }
        
        return dto;
    }
}
