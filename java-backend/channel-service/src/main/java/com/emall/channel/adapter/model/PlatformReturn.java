package com.emall.channel.adapter.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record PlatformReturn(
        String platformReturnId,
        String platformOrderId,
        String channelCode,
        String status,
        String reason,
        BigDecimal refundAmount,
        String returnTrackingNo,
        Instant createdTime,
        Instant processedTime,
        Map<String, Object> rawData
) {
}
