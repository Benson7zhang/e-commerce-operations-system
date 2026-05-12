package com.emall.common.feign.fallback;

import com.emall.common.feign.OrderFeignClient;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class OrderFeignFallback implements FallbackFactory<OrderFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(OrderFeignFallback.class);

    @Override
    public OrderFeignClient create(Throwable cause) {
        log.warn("OrderFeignClient fallback triggered: {}", cause.getMessage());
        return new OrderFeignClient() {
            @Override
            public Map<String, Object> listOrders(int page, int limit, String search, String status) {
                log.warn("Fallback: listOrders() returning empty result");
                return Map.of("data", Collections.emptyList(), "pagination", Map.of("page", 1, "limit", limit, "total", 0, "pages", 0));
            }

            @Override
            public Map<String, Object> getOrder(String orderNo) {
                log.warn("Fallback: getOrder({}) returning not found", orderNo);
                return Map.of("error", "订单服务暂不可用");
            }

            @Override
            public Map<String, Object> getOrderStats() {
                log.warn("Fallback: getOrderStats() returning default");
                return Map.of("total", 0, "status_counts", Collections.emptyMap(), "revenue", "0");
            }
        };
    }
}
