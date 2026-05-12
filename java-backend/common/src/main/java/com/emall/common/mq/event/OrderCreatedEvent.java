package com.emall.common.mq.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        Long orderId,
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        List<OrderItem> items,
        LocalDateTime createdAt
) {
    public record OrderItem(
            Long productId,
            String productName,
            Integer quantity,
            BigDecimal unitPrice
    ) {
    }
}
