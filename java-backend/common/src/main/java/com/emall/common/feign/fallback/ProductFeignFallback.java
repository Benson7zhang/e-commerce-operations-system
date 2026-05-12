package com.emall.common.feign.fallback;

import com.emall.common.feign.ProductFeignClient;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductFeignFallback implements FallbackFactory<ProductFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(ProductFeignFallback.class);

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.warn("ProductFeignClient fallback triggered: {}", cause.getMessage());
        return new ProductFeignClient() {
            @Override
            public Map<String, Object> listProducts(int page, int limit, String search, String typeFilter, String status) {
                log.warn("Fallback: listProducts() returning empty result");
                return Map.of("data", Collections.emptyList(), "pagination", Map.of("page", 1, "limit", limit, "total", 0, "pages", 0));
            }

            @Override
            public Map<String, Object> getProduct(long id) {
                log.warn("Fallback: getProduct({}) returning default unavailable product", id);
                return Map.of(
                        "id", id,
                        "name", "商品暂不可用",
                        "status", "下架",
                        "available", 0,
                        "unit_price", "0.00"
                );
            }

            @Override
            public Map<String, Object> getProductStats() {
                log.warn("Fallback: getProductStats() returning default");
                return Map.of("total", 0, "status_counts", Collections.emptyMap());
            }
        };
    }
}
