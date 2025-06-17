package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eyespire.eyespireapi.model.enums.AvailabilityStatus;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DoctorTimeSlotDTO {
    private String time;                // Thời gian hiển thị (ví dụ: "08:00")
    private LocalTime startTime;        // Thời gian bắt đầu
    private LocalTime endTime;          // Thời gian kết thúc
    private AvailabilityStatus status;  // Trạng thái khả dụng
    
    // Constructor tiện ích để tạo từ thời gian và trạng thái
    public DoctorTimeSlotDTO(LocalTime startTime, LocalTime endTime, AvailabilityStatus status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.time = startTime.toString();
    }
}
