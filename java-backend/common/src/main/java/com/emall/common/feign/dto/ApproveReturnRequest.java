package com.emall.common.feign.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ApproveReturnRequest(@JsonProperty("refund_amount") BigDecimal refundAmount) {
}
