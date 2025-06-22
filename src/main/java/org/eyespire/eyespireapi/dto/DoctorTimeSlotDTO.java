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
    private int availableCount = 3;     // Số lượng slot có sẵn cho mỗi khung giờ, mặc định là 3
    
    // Constructor tiện ích để tạo từ thời gian và trạng thái
    public DoctorTimeSlotDTO(LocalTime startTime, LocalTime endTime, AvailabilityStatus status) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.time = startTime.toString();
        this.availableCount = 3;        // Mặc định mỗi khung giờ có thể phục vụ 3 bệnh nhân
    }
    
    /**
     * Giảm số lượng slot có sẵn và cập nhật trạng thái nếu cần
     */
    public void decreaseAvailableCount() {
        if (availableCount > 0) {
            availableCount--;
            // Nếu không còn slot nào, đánh dấu là đã đặt hết
            if (availableCount == 0) {
                status = AvailabilityStatus.BOOKED;
            }
        }
    }
    
    /**
     * Kiểm tra xem khung giờ còn slot trống không
     */
    public boolean isAvailable() {
        return status == AvailabilityStatus.AVAILABLE && availableCount > 0;
    }
}
