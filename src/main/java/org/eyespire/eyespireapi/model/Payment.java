package org.eyespire.eyespireapi.model;

import org.eyespire.eyespireapi.model.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "payments")
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String transactionNo;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "payos_transaction_id")
    private String payosTransactionId;

    @Column(name = "payos_response_code")
    private String payosResponseCode;

    @Column(name = "payos_payment_method")
    private String payosPaymentMethod;

    @Column(name = "payos_bank_code")
    private String payosBankCode;

    @Column(name = "payos_card_number")
    private String payosCardNumber;

    @Column(name = "payos_card_type")
    private String payosCardType;

    @Column(name = "payos_pay_date")
    private String payosPayDate;

    @Column(name = "payos_order_info")
    private String payosOrderInfo;

    @Column(name = "return_url")
    private String returnUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "doctor_id")
    private Integer doctorId;

    @Column(name = "service_id")
    private Integer serviceId;

    @Column(name = "appointment_date")
    private String appointmentDate;

    @Column(name = "time_slot")
    private String timeSlot;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "patient_name")
    private String patientName;

    @Column(name = "patient_email")
    private String patientEmail;

    @Column(name = "patient_phone")
    private String patientPhone;

    private String notes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
