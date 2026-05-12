package com.emall.order.web;

import com.emall.order.domain.OrderEntity;
import com.emall.order.service.OrderService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public Map<String, Object> listOrders(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").descending());
        Page<OrderEntity> result = orderService.listPaged(search, status, pageable);

        return Map.of(
                "data", result.getContent().stream().map(this::toListRow).toList(),
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", result.getTotalElements(),
                        "pages", result.getTotalPages()
                )
        );
    }

    @GetMapping("/{orderNo}")
    public Map<String, Object> getOrder(@PathVariable String orderNo) {
        OrderEntity record = orderService.requireByOrderNo(orderNo);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("order_no", record.getOrderNo());
        payload.put("status", record.getStatus());
        payload.put("total_amount", record.getTotalAmount());
        payload.put("receiver_name", record.getReceiverName());
        payload.put("receiver_phone", record.getReceiverPhone());
        payload.put("receiver_address", record.getReceiverAddress());
        payload.put("carrier", record.getCarrier());
        payload.put("tracking_no", record.getTrackingNo());
        payload.put("current_node", record.getCurrentNode());
        payload.put("created_at", record.getCreatedAt());
        String itemName = extractItemName(record.getItemsInfo());
        payload.put("items", List.of(Map.of(
                "name", itemName,
                "price", record.getTotalAmount().divide(java.math.BigDecimal.valueOf(record.getQuantity())),
                "quantity", record.getQuantity(),
                "subtotal", record.getTotalAmount()
        )));
        payload.put("tracks", List.of(
                Map.of("node", "仓库", "status", "已发出", "remark", itemName + " 已出库，等待干线运输", "time", record.getCreatedAt()),
                Map.of("node", record.getCurrentNode(), "status", "已到达", "remark", "包裹已到达" + record.getCurrentNode(), "time", record.getCreatedAt())
        ));
        return payload;
    }

    @PostMapping("/simulate")
    public Map<String, Object> simulateOrder(@Valid @RequestBody SimulateOrderRequest request) {
        OrderEntity entity = orderService.simulateOrder(request.productId(), request.quantity());
        return Map.of(
                "order_no", entity.getOrderNo(),
                "status", entity.getStatus(),
                "total_amount", entity.getTotalAmount(),
                "receiver_name", entity.getReceiverName(),
                "carrier", entity.getCarrier(),
                "tracking_no", entity.getTrackingNo(),
                "tracks", List.of(
                        Map.of("time", entity.getCreatedAt(), "remark", extractItemName(entity.getItemsInfo()) + " 已出库，等待干线运输"),
                        Map.of("time", entity.getCreatedAt(), "remark", "包裹已到达" + entity.getCurrentNode())
                )
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return orderService.stats();
    }

    private Map<String, Object> toListRow(OrderEntity item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("order_no", item.getOrderNo());
        row.put("total_amount", item.getTotalAmount());
        row.put("status", item.getStatus());
        row.put("receiver_name", item.getReceiverName());
        row.put("receiver_phone", item.getReceiverPhone());
        row.put("receiver_address", item.getReceiverAddress());
        row.put("tracking_no", item.getTrackingNo());
        row.put("carrier", item.getCarrier());
        row.put("current_node", item.getCurrentNode());
        row.put("items_info", item.getItemsInfo());
        row.put("created_at", item.getCreatedAt());
        return row;
    }

    private String extractItemName(String itemsInfo) {
        return itemsInfo == null ? "-" : itemsInfo.replaceAll("\\(.*\\)$", "");
    }

    public record SimulateOrderRequest(@JsonProperty("product_id") Long productId, int quantity) {
    }
}
