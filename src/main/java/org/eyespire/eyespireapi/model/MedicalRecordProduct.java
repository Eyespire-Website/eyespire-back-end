package org.eyespire.eyespireapi.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "medical_record_products")
public class MedicalRecordProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "medical_record_id")
    @JsonBackReference
    private MedicalRecord medicalRecord;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column
    private Integer quantity;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalRecordProduct that = (MedicalRecordProduct) o;
        // Compare based on medicalRecord and product (using product.id for uniqueness)
        return Objects.equals(medicalRecord, that.medicalRecord) &&
                Objects.equals(product != null ? product.getId() : null, that.product != null ? that.product.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(medicalRecord, product != null ? product.getId() : null);
    }

    @Override
    public String toString() {
        return "MedicalRecordProduct{" +
                "id=" + id +
                ", productId=" + (product != null ? product.getId() : null) +
                ", quantity=" + quantity +
                '}';
    }
}