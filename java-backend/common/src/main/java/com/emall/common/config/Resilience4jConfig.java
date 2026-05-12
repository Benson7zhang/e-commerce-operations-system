package com.emall.common.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Resilience4jConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(
                        java.io.IOException.class,
                        java.net.SocketTimeoutException.class,
                        java.net.ConnectException.class,
                        feign.RetryableException.class,
                        org.springframework.web.client.ResourceAccessException.class
                )
                .ignoreExceptions(
                        com.emall.common.feign.FeignErrorDecoder.FeignClientException.class
                )
                .build();
        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public CircuitBreakerConfigCustomizer inventoryServiceCustomizer() {
        return CircuitBreakerConfigCustomizer.of("inventory-service",
                builder -> builder.slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(60));
    }

    @Bean
    public CircuitBreakerConfigCustomizer productServiceCustomizer() {
        return CircuitBreakerConfigCustomizer.of("product-service",
                builder -> builder.slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .failureRateThreshold(60));
    }

    @Bean
    public CircuitBreakerConfigCustomizer orderServiceCustomizer() {
        return CircuitBreakerConfigCustomizer.of("order-service",
                builder -> builder.waitDurationInOpenState(Duration.ofSeconds(60)));
    }

    @Bean
    public CircuitBreakerConfigCustomizer returnServiceCustomizer() {
        return CircuitBreakerConfigCustomizer.of("return-service",
                builder -> builder.waitDurationInOpenState(Duration.ofSeconds(60)));
    }
}
