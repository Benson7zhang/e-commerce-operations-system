package com.emall.channel.adapter.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PlatformProduct(
        String platformSkuId,
        String channelCode,
        String name,
        BigDecimal price,
        BigDecimal costPrice,
        String category,
        String status,
        String mainImageUrl,
        List<String> imageUrls,
        String description,
        Map<String, String> attributes,
        Map<String, Object> rawData
) {
}
