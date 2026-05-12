package com.emall.common.health;

import com.emall.common.config.InfraProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaseHealthController {

    private final String serviceName;
    private final InfraProperties infraProperties;

    public BaseHealthController(@Qualifier("serviceName") String serviceName, InfraProperties infraProperties) {
        this.serviceName = serviceName;
        this.infraProperties = infraProperties;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", serviceName,
                "status", "ok",
                "docs", "/actuator"
        );
    }

    @GetMapping("/health")
    public ServiceHealthResponse health() {
        return new ServiceHealthResponse(
                serviceName,
                "ok",
                "ok",
                Map.of(
                        "cache", Map.of("enabled", infraProperties.redisEnabled(), "provider", "redis"),
                        "messageQueue", Map.of("enabled", infraProperties.mqEnabled(), "provider", "rabbitmq"),
                        "tracing", Map.of("backend", infraProperties.traceBackend()),
                        "metrics", Map.of("backend", infraProperties.metricsBackend())
                )
        );
    }
}
