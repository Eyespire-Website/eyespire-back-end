package org.eyespire.eyespireapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "specialties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"doctors"})
public class Specialty {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(nullable = false, columnDefinition = "nvarchar(255)")
    private String name;
    
    @Column(columnDefinition = "nvarchar(max)")
    private String description;
    
    @Column(columnDefinition = "nvarchar(255)")
    private String imageUrl;
    
    @OneToMany(mappedBy = "specialty")
    @JsonIgnore
    private List<Doctor> doctors = new ArrayList<>();
}
