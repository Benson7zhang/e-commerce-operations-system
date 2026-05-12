package com.emall.common.feign;

import com.emall.common.feign.dto.ApproveReturnRequest;
import com.emall.common.feign.dto.RejectReturnRequest;
import com.emall.common.feign.fallback.ReturnFeignFallback;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "return-service", url = "${emall.services.return-url:http://127.0.0.1:9004}",
        configuration = FeignConfiguration.class, fallbackFactory = ReturnFeignFallback.class)
public interface ReturnFeignClient {

    @GetMapping("/api/returns")
    Map<String, Object> listReturns(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            @RequestParam(value = "status", required = false) String status);

    @GetMapping("/api/returns/{id}")
    Map<String, Object> getReturn(@PathVariable("id") long id);

    @PutMapping("/api/returns/{id}/approve")
    Map<String, Object> approveReturn(@PathVariable("id") long id, @RequestBody ApproveReturnRequest request);

    @PutMapping("/api/returns/{id}/reject")
    Map<String, Object> rejectReturn(@PathVariable("id") long id, @RequestBody RejectReturnRequest request);

    @PutMapping("/api/returns/{id}/complete")
    Map<String, Object> completeReturn(@PathVariable("id") long id);

    @GetMapping("/api/returns/stats")
    Map<String, Object> getReturnStats();
}
