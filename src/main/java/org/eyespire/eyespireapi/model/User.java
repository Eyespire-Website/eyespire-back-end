package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.GenderType;
import org.eyespire.eyespireapi.model.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    
    @Column(length = 255, unique = true)
    private String username;
    
    @Column(length = 255)
    private String name;
    
    @Column(length = 255, unique = true)
    private String email;
    
    @Column(length = 255)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GenderType gender;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(length = 20)
    private String phone;
    
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;
    
    @Column(length = 100)
    private String province;
    
    @Column(length = 100)
    private String district;
    
    @Column(length = 100)
    private String ward;
    
    @Column(name = "address_detail")
    private String addressDetail;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "is_google_account")
    private Boolean isGoogleAccount = false;
    
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
