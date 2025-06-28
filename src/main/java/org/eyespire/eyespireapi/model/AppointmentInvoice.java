package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointment_invoices")
public class AppointmentInvoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private BigDecimal depositAmount;
    
    @Column(name = "remaining_amount", precision = 10, scale = 2)
    private BigDecimal remainingAmount;
    
    @Column(name = "transaction_id", length = 255)
    private String transactionId;
    
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    
    @Column(name = "is_fully_paid")
    private Boolean isFullyPaid = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
