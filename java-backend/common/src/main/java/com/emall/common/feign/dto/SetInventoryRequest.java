package com.emall.common.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SetInventoryRequest(
        int quantity,
        @JsonProperty("locked_qty") Integer lockedQty
) {
}
