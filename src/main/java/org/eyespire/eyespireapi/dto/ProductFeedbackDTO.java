package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductFeedbackDTO {
    private Integer id;
    private Integer productId;
    private Integer patientId;
    private String patientName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private Boolean verified;
}