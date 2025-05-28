package org.eyespire.eyespireapi.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignupRequest {
    @NotBlank
    private String name;

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}
