package org.eyespire.eyespireapi.model.enums;

/**
 * Enum định nghĩa các trạng thái của lịch hẹn
 */
public enum AppointmentStatus {
    PENDING,    // Đang chờ xác nhận
    CONFIRMED,  // Đã xác nhận
    COMPLETED,  // Đã hoàn thành
    CANCELED,   // Đã hủy
    NO_SHOW     // Không đến
}
