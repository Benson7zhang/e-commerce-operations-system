package com.emall.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "emall.infra")
public record InfraProperties(
        boolean redisEnabled,
        boolean mqEnabled,
        String traceBackend,
        String metricsBackend
) {
}
