package com.emall.common.feign.fallback;

import com.emall.common.feign.ReturnFeignClient;
import com.emall.common.feign.dto.ApproveReturnRequest;
import com.emall.common.feign.dto.RejectReturnRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ReturnFeignFallback implements FallbackFactory<ReturnFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ReturnFeignFallback.class);

    @Override
    public ReturnFeignClient create(Throwable cause) {
        log.warn("ReturnFeignClient fallback triggered: {}", cause.getMessage());
        return new ReturnFeignClient() {
            @Override
            public Map<String, Object> listReturns(int page, int limit, String status) {
                log.warn("Fallback: listReturns() returning empty result");
                return Map.of("data", Collections.emptyList(), "pagination", Map.of("page", 1, "limit", limit, "total", 0, "pages", 0));
            }

            @Override
            public Map<String, Object> getReturn(long id) {
                log.warn("Fallback: getReturn({}) returning not found", id);
                return Map.of("error", "退货服务暂不可用");
            }

            @Override
            public Map<String, Object> approveReturn(long id, ApproveReturnRequest request) {
                log.warn("Fallback: approveReturn({}) cannot be executed", id);
                throw new UnsupportedOperationException("Return service unavailable, approveReturn cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> rejectReturn(long id, RejectReturnRequest request) {
                log.warn("Fallback: rejectReturn({}) cannot be executed", id);
                throw new UnsupportedOperationException("Return service unavailable, rejectReturn cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> completeReturn(long id) {
                log.warn("Fallback: completeReturn({}) cannot be executed", id);
                throw new UnsupportedOperationException("Return service unavailable, completeReturn cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> getReturnStats() {
                log.warn("Fallback: getReturnStats() returning default");
                return Map.of("total", 0, "status_counts", Collections.emptyMap(), "refunded", "0");
            }
        };
    }
}
