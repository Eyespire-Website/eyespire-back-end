package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eyespire.eyespireapi.model.enums.AppointmentStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"patient", "doctor", "service"})
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
    @JoinColumn(name = "doctor_id", nullable = true)
    private Doctor doctor;
    
    @ManyToOne
    @JoinColumn(name = "service_id", nullable = true)
    private MedicalService service;
    
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;
    
    @Column(name = "patient_name", nullable = false, columnDefinition = "nvarchar(255)")
    private String patientName;
    
    @Column(name = "patient_email", nullable = false, columnDefinition = "nvarchar(255)")
    private String patientEmail;
    
    @Column(name = "patient_phone", nullable = false, columnDefinition = "nvarchar(20)")
    private String patientPhone;
    
    @Column(columnDefinition = "nvarchar(max)")
    private String notes;
    
    @Column(name = "payment_id")
    private Integer paymentId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
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
