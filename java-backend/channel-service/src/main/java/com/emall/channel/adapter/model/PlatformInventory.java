package com.emall.channel.adapter.model;

public record PlatformInventory(
        String platformSkuId,
        String channelCode,
        int totalQuantity,
        int lockedQuantity,
        int availableQuantity
) {
}
