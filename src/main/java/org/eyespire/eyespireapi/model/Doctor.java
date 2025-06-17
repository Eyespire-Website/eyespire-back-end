package org.eyespire.eyespireapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doctors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String specialization;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String qualification;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String experience;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String imageUrl;
    
    @Column(columnDefinition = "nvarchar(max)")
    private String description;
    
    @ManyToOne
    @JoinColumn(name = "specialty_id")
    private Specialty specialty;
    
    @ManyToMany
    @JoinTable(
        name = "doctor_services",
        joinColumns = @JoinColumn(name = "doctor_id"),
        inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    private List<MedicalService> services = new ArrayList<>();
    
    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<DoctorAvailability> availabilities = new ArrayList<>();
    
    @OneToMany(mappedBy = "doctor")
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();
}
