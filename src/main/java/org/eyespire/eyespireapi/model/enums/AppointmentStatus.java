package org.eyespire.eyespireapi.model.enums;

/**
 * Enum định nghĩa các trạng thái của lịch hẹn
 */
public enum AppointmentStatus {
    PENDING,          // Đang chờ xác nhận
    CONFIRMED,        // Đã xác nhận
    DOCTOR_FINISHED,  // Bác sĩ đã khám xong
    WAITING_PAYMENT,  // Tạo hóa đơn sau khi bác sĩ khám xong
    COMPLETED,        // Đã hoàn thành (đã thanh toán)
    CANCELED,         // Đã hủy
    NO_SHOW           // Không đến  
}