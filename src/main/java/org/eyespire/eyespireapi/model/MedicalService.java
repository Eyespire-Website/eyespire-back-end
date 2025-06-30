package org.eyespire.eyespireapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"doctors", "appointments"})
public class MedicalService {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;
    
    @Column(columnDefinition = "nvarchar(max)")
    private String description;
    
    private BigDecimal price;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String imageUrl;
    
    private Integer duration; // Thời gian dịch vụ (phút)

    @ManyToMany(mappedBy = "services")
    @JsonIgnore
    private List<Doctor> doctors = new ArrayList<>();
    
    @OneToMany(mappedBy = "service")
    @JsonIgnore
    private List<Appointment> appointments = new ArrayList<>();
}
