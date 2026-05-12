package com.emall.common.feign;

import com.emall.common.feign.fallback.ChannelFeignFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "channel-service", url = "${emall.services.channel-url:http://127.0.0.1:9008}",
        configuration = FeignConfiguration.class, fallbackFactory = ChannelFeignFallback.class)
public interface ChannelFeignClient {

    @GetMapping("/api/channels/orders")
    Map<String, Object> listChannelOrders(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "channelCode", required = false) String channelCode,
            @RequestParam(value = "status", required = false) String status);

    @GetMapping("/api/channels/orders/{id}")
    Map<String, Object> getChannelOrder(@PathVariable("id") Long id);

    @GetMapping("/api/channels/adapters")
    Object listAdapters();

    @PostMapping("/api/channels/sync")
    Map<String, Object> syncAll();
}
