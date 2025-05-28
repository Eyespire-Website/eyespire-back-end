package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "doctors")
public class Doctor {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    @Column(length = 255)
    private String specialization;
    
    @Column(name = "certification_code", length = 100)
    private String certificationCode;
    
    @Column
    private String certifications;
    
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;
    
    @Column(name = "phone_for_services", length = 20)
    private String phoneForServices;
    
    @Column
    private String bio;
    
    @Column(name = "profile_image_url", length = 255)
    private String profileImageUrl;
}
