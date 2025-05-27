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
}
