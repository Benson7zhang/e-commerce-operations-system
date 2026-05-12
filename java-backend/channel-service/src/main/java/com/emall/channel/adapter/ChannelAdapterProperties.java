package com.emall.channel.adapter;

import java.util.Collections;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.channel")
public record ChannelAdapterProperties(
        Map<String, ChannelDefinition> adapters
) {
    public record ChannelDefinition(
            boolean enabled,
            Map<String, String> config
    ) {
        public ChannelDefinition {
            config = config == null ? Collections.emptyMap() : config;
        }
    }

    public ChannelAdapterProperties {
        adapters = adapters == null ? Collections.emptyMap() : adapters;
    }

    public ChannelDefinition getDefinition(String channelCode) {
        return adapters.get(channelCode);
    }

    public boolean isEnabled(String channelCode) {
        ChannelDefinition def = adapters.get(channelCode);
        return def != null && def.enabled();
    }
}
