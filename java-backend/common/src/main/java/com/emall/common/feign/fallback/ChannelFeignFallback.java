package com.emall.common.feign.fallback;

import com.emall.common.feign.ChannelFeignClient;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ChannelFeignFallback implements FallbackFactory<ChannelFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ChannelFeignFallback.class);

    @Override
    public ChannelFeignClient create(Throwable cause) {
        log.warn("ChannelFeignClient fallback triggered: {}", cause.getMessage());
        return new ChannelFeignClient() {
            @Override
            public Map<String, Object> listChannelOrders(int page, int limit, String channelCode, String status) {
                log.warn("Fallback: listChannelOrders() returning empty result");
                return Map.of("data", Collections.emptyList(),
                        "pagination", Map.of("page", 1, "limit", limit, "total", 0, "pages", 0));
            }

            @Override
            public Map<String, Object> getChannelOrder(Long id) {
                log.warn("Fallback: getChannelOrder({}) returning not found", id);
                return Map.of("error", "渠道订单服务暂不可用");
            }

            @Override
            public Object listAdapters() {
                log.warn("Fallback: listAdapters() returning empty list");
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> syncAll() {
                log.warn("Fallback: syncAll() returning error");
                return Map.of("error", "渠道订单服务暂不可用");
            }
        };
    }
}
