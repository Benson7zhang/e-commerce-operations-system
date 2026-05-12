package com.emall.channel.adapter.model;

public record PlatformShipment(
        String platformOrderId,
        String channelCode,
        String carrier,
        String trackingNo
) {
}
