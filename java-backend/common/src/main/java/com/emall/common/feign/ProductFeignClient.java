package com.emall.common.feign;

import com.emall.common.feign.fallback.ProductFeignFallback;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service", url = "${emall.services.product-url:http://127.0.0.1:9001}",
        configuration = FeignConfiguration.class, fallbackFactory = ProductFeignFallback.class)
public interface ProductFeignClient {

    @GetMapping("/api/products")
    Map<String, Object> listProducts(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "type_filter", required = false) String typeFilter,
            @RequestParam(value = "status", required = false) String status);

    @GetMapping("/api/products/{id}")
    Map<String, Object> getProduct(@PathVariable("id") long id);

    @GetMapping("/api/products/stats")
    Map<String, Object> getProductStats();
}
