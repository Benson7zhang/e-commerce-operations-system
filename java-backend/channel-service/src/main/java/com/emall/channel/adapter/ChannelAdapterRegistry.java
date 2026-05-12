package com.emall.channel.adapter;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
public class ChannelAdapterRegistry implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(ChannelAdapterRegistry.class);

    private final Map<String, ChannelAdapter> adapters;
    private final ChannelAdapterProperties properties;

    public ChannelAdapterRegistry(List<ChannelAdapter> adapterList, ChannelAdapterProperties properties) {
        this.properties = properties;
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(ChannelAdapter::channelCode, a -> a));
    }

    @PostConstruct
    public void initializeAll() {
        for (ChannelAdapter adapter : adapters.values()) {
            ChannelAdapterProperties.ChannelDefinition def = properties.getDefinition(adapter.channelCode());
            boolean enabled = def == null || def.enabled();
            if (!enabled) {
                log.info("Channel adapter {} is disabled, skipping", adapter.channelCode());
                continue;
            }
            Map<String, String> configMap = def != null ? def.config() : Map.of();
            ChannelAdapterConfig config = new ChannelAdapterConfig(adapter.channelCode(), configMap);
            try {
                adapter.initialize(config);
                log.info("Channel adapter {} initialized", adapter.channelCode());
            } catch (Exception e) {
                log.error("Failed to initialize channel adapter {}: {}", adapter.channelCode(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void destroy() {
        for (ChannelAdapter adapter : adapters.values()) {
            try {
                adapter.destroy();
            } catch (Exception e) {
                log.error("Failed to destroy channel adapter {}: {}", adapter.channelCode(), e.getMessage(), e);
            }
        }
    }

    public ChannelAdapter getAdapter(String channelCode) {
        return adapters.get(channelCode);
    }

    public Collection<ChannelAdapter> getAllAdapters() {
        return adapters.values();
    }
}
