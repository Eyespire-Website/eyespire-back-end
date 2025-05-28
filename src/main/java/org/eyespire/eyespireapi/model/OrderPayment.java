package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "order_payments")
public class OrderPayment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "transaction_id", length = 255)
    private String transactionId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", length = 20)
    private PaymentStatus paymentStatus;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
