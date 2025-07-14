package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import org.eyespire.eyespireapi.model.enums.PaymentType;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "order_payments")
@NoArgsConstructor
@AllArgsConstructor
public class OrderPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String transactionNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    // ADDED: New field to store PaymentType
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
    
    @Column(name = "payos_transaction_id")
    private String payosTransactionId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // ADDED: Set paymentType based on payosTransactionId
        this.paymentType = (payosTransactionId != null && !payosTransactionId.isEmpty()) ? PaymentType.PAYOS : PaymentType.CASH;
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
