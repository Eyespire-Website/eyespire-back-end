package org.eyespire.eyespireapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceCategory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, unique = true, columnDefinition = "nvarchar(255)")
    private String name;
    
    @Column(columnDefinition = "nvarchar(max)")
    private String description;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String iconUrl;
    
    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private List<MedicalService> services = new ArrayList<>();
}
