package com.emall.channel.web;

import com.emall.channel.adapter.ChannelAdapter;
import com.emall.channel.adapter.ChannelAdapterRegistry;
import com.emall.channel.adapter.model.PlatformShipment;
import com.emall.channel.domain.ChannelOrderEntity;
import com.emall.channel.repository.ChannelOrderRepository;
import com.emall.channel.service.ChannelSyncService;
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
@RequestMapping("/api/channels")
public class ChannelOrderController {

    private final ChannelOrderRepository repository;
    private final ChannelAdapterRegistry adapterRegistry;
    private final ChannelSyncService syncService;

    public ChannelOrderController(ChannelOrderRepository repository, ChannelAdapterRegistry adapterRegistry,
            ChannelSyncService syncService) {
        this.repository = repository;
        this.adapterRegistry = adapterRegistry;
        this.syncService = syncService;
    }

    @GetMapping("/orders")
    public Map<String, Object> listOrders(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("orderTime").descending());
        Page<ChannelOrderEntity> result = repository.findByFilters(
                (channelCode == null || channelCode.isEmpty()) ? null : channelCode,
                (status == null || status.isEmpty()) ? null : status,
                pageable);

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

    @GetMapping("/orders/{id}")
    public Map<String, Object> getOrder(@PathVariable Long id) {
        ChannelOrderEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("渠道订单不存在"));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", entity.getId());
        payload.put("channel_code", entity.getChannelCode());
        payload.put("platform_order_id", entity.getPlatformOrderId());
        payload.put("unified_status", entity.getUnifiedStatus());
        payload.put("platform_status", entity.getPlatformStatus());
        payload.put("total_amount", entity.getTotalAmount());
        payload.put("buyer_name", entity.getBuyerName());
        payload.put("buyer_phone", entity.getBuyerPhone());
        payload.put("buyer_address", entity.getBuyerAddress());
        payload.put("items_summary", entity.getItemsSummary());
        payload.put("carrier", entity.getCarrier());
        payload.put("tracking_no", entity.getTrackingNo());
        payload.put("order_time", entity.getOrderTime());
        payload.put("synced_at", entity.getSyncedAt());
        return payload;
    }

    @GetMapping("/adapters")
    public List<Map<String, Object>> listAdapters() {
        return adapterRegistry.getAllAdapters().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("channel_code", a.channelCode());
                    m.put("channel_name", a.channelName());
                    m.put("connected", a.testConnection());
                    return m;
                })
                .toList();
    }

    @PostMapping("/sync")
    public Map<String, Object> syncAll() {
        return syncService.syncAllChannels();
    }

    @PostMapping("/sync/{channelCode}")
    public Map<String, Object> syncChannel(@PathVariable String channelCode) {
        int count = syncService.syncChannel(channelCode);
        return Map.of("channel", channelCode, "synced", count);
    }

    @PostMapping("/sync/{channelCode}/products")
    public Map<String, Object> syncProducts(@PathVariable String channelCode) {
        int count = syncService.syncProducts(channelCode);
        return Map.of("channel", channelCode, "synced_products", count);
    }

    @PostMapping("/sync/{channelCode}/returns")
    public Map<String, Object> syncReturns(@PathVariable String channelCode) {
        int count = syncService.syncReturns(channelCode);
        return Map.of("channel", channelCode, "synced_returns", count);
    }

    @PostMapping("/shipments")
    public Map<String, Object> pushShipment(@Valid @RequestBody ShipmentRequest request) {
        ChannelAdapter adapter = adapterRegistry.getAdapter(request.channelCode());
        if (adapter == null) {
            return Map.of("success", false, "error", "未找到渠道适配器: " + request.channelCode());
        }
        PlatformShipment shipment = new PlatformShipment(
                request.platformOrderId(), request.channelCode(),
                request.carrier(), request.trackingNo()
        );
        adapter.pushShipment(shipment);
        return Map.of("success", true, "channel", request.channelCode());
    }

    @GetMapping("/test/{channelCode}")
    public Map<String, Object> testConnection(@PathVariable String channelCode) {
        ChannelAdapter adapter = adapterRegistry.getAdapter(channelCode);
        if (adapter == null) {
            return Map.of("channel", channelCode, "connected", false, "error", "未找到适配器");
        }
        boolean ok = adapter.testConnection();
        return Map.of("channel", channelCode, "channel_name", adapter.channelName(), "connected", ok);
    }

    private Map<String, Object> toListRow(ChannelOrderEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("channel_code", entity.getChannelCode());
        row.put("platform_order_id", entity.getPlatformOrderId());
        row.put("unified_status", entity.getUnifiedStatus());
        row.put("platform_status", entity.getPlatformStatus());
        row.put("total_amount", entity.getTotalAmount());
        row.put("buyer_name", entity.getBuyerName());
        row.put("items_summary", entity.getItemsSummary());
        row.put("carrier", entity.getCarrier());
        row.put("tracking_no", entity.getTrackingNo());
        row.put("order_time", entity.getOrderTime());
        row.put("synced_at", entity.getSyncedAt());
        return row;
    }

    public record ShipmentRequest(
            String platformOrderId,
            String channelCode,
            String carrier,
            String trackingNo
    ) {
    }
}
