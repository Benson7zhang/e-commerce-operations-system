package com.emall.channel.adapter;

import java.util.Collections;
import java.util.Map;

public record ChannelAdapterConfig(
        String channelCode,
        Map<String, String> properties
) {
    public ChannelAdapterConfig {
        properties = properties == null ? Collections.emptyMap() : Map.copyOf(properties);
    }

    public String get(String key) {
        return properties.get(key);
    }

    public String get(String key, String defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
}
