package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private String village;
    private String gender;
    private String dateOfBirth;
}