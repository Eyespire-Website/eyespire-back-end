package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Integer id;
    private String username;
    private String name;
    private String email;
    private String role;
    private String gender;
    private String phone;
    private String avatarUrl;
    private Boolean isGoogleAccount;
    
    public LoginResponse(Integer id, String username, String name, String email, String role, String gender, String phone, String avatarUrl) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.gender = gender;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.isGoogleAccount = false;
    }
}
