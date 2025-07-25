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
    
    @Column(length = 255, unique = true, columnDefinition = "nvarchar(255)")
    private String username;
    
    @Column(length = 255, columnDefinition = "nvarchar(255)")
    private String name;
    
    @Column(length = 255, unique = true, columnDefinition = "nvarchar(255)")
    private String email;
    
    @Column(length = 255, columnDefinition = "nvarchar(255)")
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private GenderType gender;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(length = 20, columnDefinition = "nvarchar(20)")
    private String phone;
    
    @Column(name = "avatar_url", length = 255, columnDefinition = "nvarchar(255)")
    private String avatarUrl;
    
    @Column(length = 100, columnDefinition = "nvarchar(100)")
    private String province;
    
    @Column(length = 100, columnDefinition = "nvarchar(100)")
    private String district;
    
    @Column(length = 100, columnDefinition = "nvarchar(100)")
    private String ward;
    
    @Column(name = "address_detail", columnDefinition = "nvarchar(255)")
    private String addressDetail;
    
    @Column(length = 20, columnDefinition = "nvarchar(20)")
    private String status = "active"; // Mặc định là active
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
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
        if (status == null) {
            status = "active";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", password='" + password + '\'' +
                ", role=" + role +
                ", gender=" + gender +
                ", dateOfBirth=" + dateOfBirth +
                ", phone='" + phone + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", province='" + province + '\'' +
                ", district='" + district + '\'' +
                ", ward='" + ward + '\'' +
                ", addressDetail='" + addressDetail + '\'' +
                ", status='" + status + '\'' +
                ", lastLogin=" + lastLogin +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isGoogleAccount=" + isGoogleAccount +
                '}';
    }
}
