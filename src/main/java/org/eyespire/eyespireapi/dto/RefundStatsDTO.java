package org.eyespire.eyespireapi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundStatsDTO {
    private Long totalRefunds;
    private BigDecimal totalAmount;
    private Long pendingRefunds;
    private Long completedRefunds;
    private Map<String, Long> methodBreakdown;
}
