package com.emall.returnservice.web;

import com.emall.returnservice.domain.ReturnEntity;
import com.emall.returnservice.service.ReturnService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/returns")
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @GetMapping
    public Map<String, Object> listReturns(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").descending());
        Page<ReturnEntity> result = returnService.listPaged(status, pageable);

        return Map.of(
                "data", result.getContent().stream().map(this::toView).toList(),
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", result.getTotalElements(),
                        "pages", result.getTotalPages()
                )
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return returnService.returnStats();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getDetail(@PathVariable long id) {
        return toView(returnService.require(id));
    }

    @PutMapping("/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, @Valid @RequestBody ApproveRequest request) {
        ReturnEntity entity = returnService.approve(id, request.refundAmount());
        return Map.of("message", "approved", "refund_amount", entity.getRefundAmount());
    }

    @PutMapping("/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id, @Valid @RequestBody RejectRequest request) {
        returnService.reject(id, request.rejectReason());
        return Map.of("message", "rejected");
    }

    @PutMapping("/{id}/complete")
    public Map<String, Object> complete(@PathVariable long id) {
        ReturnEntity entity = returnService.complete(id);
        return Map.of("message", "completed", "status", entity.getStatus());
    }

    private Map<String, Object> toView(ReturnEntity item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("order_id", item.getOrderId());
        row.put("order_no", item.getOrderNo());
        row.put("receiver_name", item.getReceiverName());
        row.put("total_amount", item.getTotalAmount());
        row.put("reason", item.getReason());
        row.put("status", item.getStatus());
        row.put("refund_amount", item.getRefundAmount());
        row.put("reject_reason", item.getRejectReason());
        row.put("created_at", item.getCreatedAt());
        return row;
    }

    public record ApproveRequest(@JsonProperty("refund_amount") BigDecimal refundAmount) {
    }

    public record RejectRequest(@JsonProperty("reject_reason") String rejectReason) {
    }
}
