package org.eyespire.eyespireapi.model.enums;

/**
 * Enum định nghĩa các trạng thái của lịch hẹn
 */
public enum AppointmentStatus {
    PENDING,         // Đang chờ xác nhận
    CONFIRMED,       // Đã xác nhận
    WAITING_PAYMENT, // Chờ thanh toán (sau khi bác sĩ tạo hồ sơ bệnh án)
    COMPLETED,       // Đã hoàn thành (đã thanh toán)
    CANCELED,        // Đã hủy
    NO_SHOW          // Không đến
}
