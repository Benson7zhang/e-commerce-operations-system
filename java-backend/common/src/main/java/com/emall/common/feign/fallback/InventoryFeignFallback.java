package com.emall.common.feign.fallback;

import com.emall.common.feign.InventoryFeignClient;
import com.emall.common.feign.dto.QuantityRequest;
import com.emall.common.feign.dto.SetInventoryRequest;
import com.emall.common.feign.dto.UpsertInventoryRequest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class InventoryFeignFallback implements FallbackFactory<InventoryFeignClient> {

    private static final Logger log = LoggerFactory.getLogger(InventoryFeignFallback.class);

    @Override
    public InventoryFeignClient create(Throwable cause) {
        log.warn("InventoryFeignClient fallback triggered: {}", cause.getMessage());
        return new InventoryFeignClient() {
            @Override
            public List<Map<String, Object>> alerts() {
                log.warn("Fallback: alerts() returning empty list");
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> summary() {
                log.warn("Fallback: summary() returning empty map");
                return Map.of();
            }

            @Override
            public List<Map<String, Object>> listInventory() {
                log.warn("Fallback: listInventory() returning empty list");
                return Collections.emptyList();
            }

            @Override
            public Map<String, Object> getInventory(long productId) {
                log.warn("Fallback: getInventory({}) returning default", productId);
                return Map.of(
                        "product_id", productId,
                        "warehouse_id", 0L,
                        "warehouse_name", "unknown",
                        "quantity", 0,
                        "locked_qty", 0,
                        "available", 0,
                        "alert_threshold", 10,
                        "unit_cost", BigDecimal.ZERO
                );
            }

            @Override
            public Map<String, Object> upsertInventory(UpsertInventoryRequest request) {
                log.warn("Fallback: upsertInventory() for productId={}", request.productId());
                throw new UnsupportedOperationException("Inventory service unavailable, upsertInventory cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> setInventory(long productId, SetInventoryRequest request) {
                log.warn("Fallback: setInventory({}) cannot be executed", productId);
                throw new UnsupportedOperationException("Inventory service unavailable, setInventory cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> increaseInventory(long productId, QuantityRequest request) {
                log.warn("Fallback: increaseInventory({}) cannot be executed", productId);
                throw new UnsupportedOperationException("Inventory service unavailable, increaseInventory cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> decreaseInventory(long productId, QuantityRequest request) {
                log.warn("Fallback: decreaseInventory({}) cannot be executed", productId);
                throw new UnsupportedOperationException("Inventory service unavailable, decreaseInventory cannot be executed in fallback mode");
            }

            @Override
            public Map<String, Object> deleteInventory(long productId) {
                log.warn("Fallback: deleteInventory({}) returning success", productId);
                return Map.of("message", "fallback_deleted");
            }
        };
    }
}
