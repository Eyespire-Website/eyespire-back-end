package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho việc tạo hoặc cập nhật thông tin bác sĩ
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorDTO {
    private Integer id;
    private String name;
    private String specialization;
    private String qualification;
    private String experience;
    private String imageUrl;
    private String description;
    private Integer userId;
    private Integer specialtyId;
}