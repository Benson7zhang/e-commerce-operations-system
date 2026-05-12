package com.emall.channel.service;

import com.emall.channel.adapter.ChannelAdapter;
import com.emall.channel.adapter.ChannelAdapterException;
import com.emall.channel.adapter.ChannelAdapterRegistry;
import com.emall.channel.adapter.model.PageResult;
import com.emall.channel.adapter.model.PlatformOrder;
import com.emall.channel.adapter.model.PlatformProduct;
import com.emall.channel.adapter.model.PlatformReturn;
import com.emall.channel.domain.ChannelOrderEntity;
import com.emall.channel.repository.ChannelOrderRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelSyncService {

    private static final Logger log = LoggerFactory.getLogger(ChannelSyncService.class);

    private final ChannelAdapterRegistry adapterRegistry;
    private final ChannelOrderRepository repository;

    public ChannelSyncService(ChannelAdapterRegistry adapterRegistry, ChannelOrderRepository repository) {
        this.adapterRegistry = adapterRegistry;
        this.repository = repository;
    }

    public Map<String, Object> syncAllChannels() {
        List<String> results = new ArrayList<>();
        for (ChannelAdapter adapter : adapterRegistry.getAllAdapters()) {
            try {
                int count = syncChannel(adapter.channelCode());
                results.add(adapter.channelName() + ": 同步 " + count + " 笔");
            } catch (Exception e) {
                log.error("Sync failed for channel {}: {}", adapter.channelCode(), e.getMessage());
                results.add(adapter.channelName() + ": 同步失败 - " + e.getMessage());
            }
        }
        return Map.of("results", results);
    }

    @Transactional
    public int syncChannel(String channelCode) {
        ChannelAdapter adapter = requireAdapter(channelCode);

        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(30, java.time.temporal.ChronoUnit.DAYS);

        int totalSynced = 0;
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            PageResult<PlatformOrder> pageResult = adapter.fetchOrders(startTime, endTime, page, 50);

            for (PlatformOrder po : pageResult.data()) {
                upsertOrder(po);
                totalSynced++;
            }

            hasMore = pageResult.hasMore();
            page++;
        }

        log.info("Channel {} synced {} orders", channelCode, totalSynced);
        return totalSynced;
    }

    @Transactional
    public int syncProducts(String channelCode) {
        ChannelAdapter adapter = requireAdapter(channelCode);

        int totalSynced = 0;
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            PageResult<PlatformProduct> pageResult = adapter.fetchProducts(page, 50);
            totalSynced += pageResult.data().size();
            hasMore = pageResult.hasMore();
            page++;
        }

        log.info("Channel {} synced {} products", channelCode, totalSynced);
        return totalSynced;
    }

    @Transactional
    public int syncReturns(String channelCode) {
        ChannelAdapter adapter = requireAdapter(channelCode);

        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(30, java.time.temporal.ChronoUnit.DAYS);

        int totalSynced = 0;
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            PageResult<PlatformReturn> pageResult = adapter.fetchReturns(startTime, endTime, page, 50);
            totalSynced += pageResult.data().size();
            hasMore = pageResult.hasMore();
            page++;
        }

        log.info("Channel {} synced {} returns", channelCode, totalSynced);
        return totalSynced;
    }

    private ChannelAdapter requireAdapter(String channelCode) {
        ChannelAdapter adapter = adapterRegistry.getAdapter(channelCode);
        if (adapter == null) {
            throw new ChannelAdapterException(channelCode, "sync", "未找到渠道适配器");
        }
        return adapter;
    }

    private void upsertOrder(PlatformOrder po) {
        ChannelOrderEntity entity = repository
                .findByChannelCodeAndPlatformOrderId(po.channelCode(), po.platformOrderId())
                .map(existing -> updateEntity(existing, po))
                .orElseGet(() -> createEntity(po));
        repository.save(entity);
    }

    private ChannelOrderEntity createEntity(PlatformOrder po) {
        return new ChannelOrderEntity(
                null,
                po.channelCode(),
                po.platformOrderId(),
                mapStatus(po.status()),
                po.status(),
                po.totalAmount(),
                po.buyer() != null ? po.buyer().name() : "",
                po.buyer() != null ? po.buyer().phone() : "",
                po.buyer() != null ? po.buyer().address() : "",
                buildItemsSummary(po),
                po.shipping() != null ? po.shipping().carrier() : "",
                po.shipping() != null ? po.shipping().trackingNo() : "",
                po.orderTime(),
                Instant.now(),
                po.rawData() != null ? po.rawData().toString() : ""
        );
    }

    private ChannelOrderEntity updateEntity(ChannelOrderEntity entity, PlatformOrder po) {
        return new ChannelOrderEntity(
                entity.getId(),
                po.channelCode(),
                po.platformOrderId(),
                mapStatus(po.status()),
                po.status(),
                po.totalAmount(),
                po.buyer() != null ? po.buyer().name() : entity.getBuyerName(),
                po.buyer() != null ? po.buyer().phone() : entity.getBuyerPhone(),
                po.buyer() != null ? po.buyer().address() : entity.getBuyerAddress(),
                buildItemsSummary(po),
                po.shipping() != null && po.shipping().carrier() != null && !po.shipping().carrier().isEmpty()
                        ? po.shipping().carrier() : entity.getCarrier(),
                po.shipping() != null && po.shipping().trackingNo() != null && !po.shipping().trackingNo().isEmpty()
                        ? po.shipping().trackingNo() : entity.getTrackingNo(),
                po.orderTime(),
                Instant.now(),
                po.rawData() != null ? po.rawData().toString() : entity.getRawDataJson()
        );
    }

    private String mapStatus(String platformStatus) {
        if (platformStatus == null) return "PENDING";
        return switch (platformStatus) {
            case "已付款", "待发货" -> "PAID";
            case "已发货", "配送中" -> "SHIPPED";
            case "已完成", "已签收" -> "COMPLETED";
            case "已取消" -> "CANCELLED";
            case "已退款", "退款中" -> "REFUNDED";
            case "待付款" -> "PENDING";
            default -> "PENDING";
        };
    }

    private String buildItemsSummary(PlatformOrder po) {
        if (po.items() == null || po.items().isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < po.items().size(); i++) {
            PlatformOrder.OrderItem item = po.items().get(i);
            if (i > 0) sb.append(", ");
            sb.append(item.name()).append(" x").append(item.quantity());
        }
        return sb.toString();
    }
}
