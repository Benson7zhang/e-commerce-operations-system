package com.emall.common.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record UpsertInventoryRequest(
        @JsonProperty("product_id") long productId,
        @JsonProperty("product_name") String productName,
        @JsonProperty("warehouse_id") long warehouseId,
        @JsonProperty("warehouse_name") String warehouseName,
        int quantity,
        @JsonProperty("locked_qty") int lockedQty,
        @JsonProperty("alert_threshold") int alertThreshold,
        @JsonProperty("unit_cost") BigDecimal unitCost
) {
}
