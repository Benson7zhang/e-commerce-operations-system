package com.emall.common.mq.event;

public record InventoryChangedEvent(
        Long productId,
        ChangeType changeType,
        Integer quantity,
        Integer beforeQty,
        Integer afterQty,
        String refType,
        String refId
) {
    public enum ChangeType {
        INCREASE, DECREASE, LOCK, UNLOCK
    }
}
