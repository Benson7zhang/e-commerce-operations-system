package com.emall.channel.adapter.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PlatformOrder(
        String platformOrderId,
        String channelCode,
        String status,
        String returnStatus,
        BigDecimal totalAmount,
        List<OrderItem> items,
        BuyerInfo buyer,
        ShippingInfo shipping,
        Instant orderTime,
        Instant paidTime,
        Instant shippedTime,
        String remark,
        Map<String, Object> rawData
) {
    public record OrderItem(String skuId, String name, int quantity, BigDecimal unitPrice) {}
    public record BuyerInfo(String name, String phone, String address) {}
    public record ShippingInfo(String carrier, String trackingNo) {}
}
