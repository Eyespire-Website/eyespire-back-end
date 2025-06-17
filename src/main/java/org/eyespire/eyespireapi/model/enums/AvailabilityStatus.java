package org.eyespire.eyespireapi.model.enums;

/**
 * Enum định nghĩa các trạng thái khả dụng của bác sĩ
 */
public enum AvailabilityStatus {
    AVAILABLE,  // Có sẵn để đặt lịch
    BOOKED,     // Đã được đặt lịch
    UNAVAILABLE // Không có sẵn (nghỉ phép, bận việc khác)
}
