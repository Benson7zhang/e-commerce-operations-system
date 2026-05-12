package com.emall.common.mq.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReturnCompletedEvent(
        Long returnId,
        Long orderId,
        Long productId,
        Integer quantity,
        BigDecimal refundAmount,
        LocalDateTime createdAt
) {
}
