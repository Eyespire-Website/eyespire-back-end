package org.eyespire.eyespireapi.model.enums;

/**
 * Enum định nghĩa các trạng thái hoàn tiền
 */
public enum RefundStatus {
    PENDING_MANUAL_REFUND,  // Chờ hoàn tiền thủ công
    COMPLETED,              // Đã hoàn tiền
    REJECTED                // Từ chối hoàn tiền
}
