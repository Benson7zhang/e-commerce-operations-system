package com.emall.common.feign;

import com.emall.common.feign.dto.QuantityRequest;
import com.emall.common.feign.dto.SetInventoryRequest;
import com.emall.common.feign.dto.UpsertInventoryRequest;
import com.emall.common.feign.fallback.InventoryFeignFallback;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service", url = "${emall.services.inventory-url:http://127.0.0.1:9003}",
        configuration = FeignConfiguration.class, fallbackFactory = InventoryFeignFallback.class)
public interface InventoryFeignClient {

    @GetMapping("/api/inventory/alerts")
    List<Map<String, Object>> alerts();

    @GetMapping("/api/inventory/summary")
    Map<String, Object> summary();

    @GetMapping("/api/inventory/products")
    List<Map<String, Object>> listInventory();

    @GetMapping("/api/inventory/products/{productId}")
    Map<String, Object> getInventory(@PathVariable("productId") long productId);

    @PostMapping("/api/inventory/products")
    Map<String, Object> upsertInventory(@RequestBody UpsertInventoryRequest request);

    @PutMapping("/api/inventory/products/{productId}/set")
    Map<String, Object> setInventory(
            @PathVariable("productId") long productId,
            @RequestBody SetInventoryRequest request);

    @PutMapping("/api/inventory/products/{productId}/increase")
    Map<String, Object> increaseInventory(
            @PathVariable("productId") long productId,
            @RequestBody QuantityRequest request);

    @PutMapping("/api/inventory/products/{productId}/decrease")
    Map<String, Object> decreaseInventory(
            @PathVariable("productId") long productId,
            @RequestBody QuantityRequest request);

    @DeleteMapping("/api/inventory/products/{productId}")
    Map<String, Object> deleteInventory(@PathVariable("productId") long productId);
}
