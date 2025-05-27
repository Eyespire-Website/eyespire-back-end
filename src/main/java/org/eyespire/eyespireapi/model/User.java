package org.eyespire.eyespireapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    @Column(length = 50)
    private String role;
    
    @Column(length = 20)
    private String gender;
    
    @Column(length = 20)
    private String phone;
    
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;
    
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
