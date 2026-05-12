package com.emall.common.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RejectReturnRequest(@JsonProperty("reject_reason") String rejectReason) {
}
