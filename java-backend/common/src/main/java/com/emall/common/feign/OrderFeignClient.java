package com.emall.common.feign;

import com.emall.common.feign.fallback.OrderFeignFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", url = "${emall.services.order-url:http://127.0.0.1:9002}",
        configuration = FeignConfiguration.class, fallbackFactory = OrderFeignFallback.class)
public interface OrderFeignClient {

    @GetMapping("/api/orders")
    Map<String, Object> listOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status);

    @GetMapping("/api/orders/{orderNo}")
    Map<String, Object> getOrder(@PathVariable("orderNo") String orderNo);

    @GetMapping("/api/orders/stats")
    Map<String, Object> getOrderStats();
}
