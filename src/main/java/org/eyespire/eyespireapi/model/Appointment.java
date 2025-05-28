package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "appointments")
public class Appointment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private User patient;
    
    @ManyToOne
    @JoinColumn(name = "doctor_id")
    private Doctor doctor;
    
    @OneToOne
    @JoinColumn(name = "availability_id", unique = true)
    private DoctorAvailability availability;
    
    @ManyToOne
    @JoinColumn(name = "service_id")
    private MedicalService service;
    
    @Column(name = "appointment_date")
    private LocalDate appointmentDate;
    
    @Column(name = "hour_slot")
    private Integer hourSlot;
    
    @Column(name = "booking_fee", precision = 10, scale = 2)
    private BigDecimal bookingFee;
    
    @Column(name = "service_price", precision = 10, scale = 2)
    private BigDecimal servicePrice;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AppointmentStatus status;
    
    @Column
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
