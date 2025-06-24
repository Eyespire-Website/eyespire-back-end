package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorAvailabilityDTO {
    
    private Integer id;
    private Integer doctorId;
    private String doctorName;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private AvailabilityStatus status;
    private String notes;
}
