package org.eyespire.eyespireapi.dto;

import java.math.BigDecimal;
import java.util.List;

public class InvoiceCreationRequestDTO {
    private List<Integer> serviceIds;
    private List<MedicationDTO> medications;
    private Boolean includeMedications;

    // Getters and Setters
    public List<Integer> getServiceIds() {
        return serviceIds;
    }

    public void setServiceIds(List<Integer> serviceIds) {
        this.serviceIds = serviceIds;
    }
    // THÊM: Getter và Setter cho includeMedications
    public Boolean getIncludeMedications() {
        return includeMedications;
    }

    public List<MedicationDTO> getMedications() {
        return medications;
    }

    public void setMedications(List<MedicationDTO> medications) {
        this.medications = medications;
    }

    public static class MedicationDTO {
        private Integer productId;
        private Integer quantity;
        private BigDecimal price;

        // Getters and Setters
        public Integer getProductId() {
            return productId;
        }

        public void setProductId(Integer productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }
    }
}