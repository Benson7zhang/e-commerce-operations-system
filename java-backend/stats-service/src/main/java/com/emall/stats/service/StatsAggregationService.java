package com.emall.stats.service;

import com.emall.common.feign.ChannelFeignClient;
import com.emall.common.feign.InventoryFeignClient;
import com.emall.common.feign.OrderFeignClient;
import com.emall.common.feign.ProductFeignClient;
import com.emall.common.feign.ReturnFeignClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StatsAggregationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ProductFeignClient productClient;
    private final OrderFeignClient orderClient;
    private final InventoryFeignClient inventoryClient;
    private final ReturnFeignClient returnClient;
    private final ChannelFeignClient channelClient;

    public StatsAggregationService(ProductFeignClient productClient, OrderFeignClient orderClient,
                                   InventoryFeignClient inventoryClient, ReturnFeignClient returnClient,
                                   ChannelFeignClient channelClient) {
        this.productClient = productClient;
        this.orderClient = orderClient;
        this.inventoryClient = inventoryClient;
        this.returnClient = returnClient;
        this.channelClient = channelClient;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> stats() {
        Map<String, Object> productStats = safeProductStats();
        Map<String, Object> orderStats = safeOrderStats();
        Map<String, Object> returnStats = safeReturnStats();
        Map<String, Object> inventorySummary = safeSummary();
        Map<String, Object> channelOrders = aggregateChannelOrders();

        Map<String, Long> productStatusCounts = (Map<String, Long>) productStats.getOrDefault("status_counts", Collections.emptyMap());
        Map<String, Long> orderStatusCounts = (Map<String, Long>) orderStats.getOrDefault("status_counts", Collections.emptyMap());

        return Map.of(
                "products", Map.of(
                        "total", productStats.getOrDefault("total", 0),
                        "on_sale", productStatusCounts.getOrDefault("在售", 0L)
                ),
                "orders", Map.of(
                        "total", orderStats.getOrDefault("total", 0),
                        "received", orderStatusCounts.getOrDefault("已签收", 0L),
                        "in_transit", orderStatusCounts.getOrDefault("配送中", 0L),
                        "detained", orderStatusCounts.getOrDefault("滞留", 0L),
                        "returned", orderStatusCounts.getOrDefault("已退货", 0L),
                        "return_requested", orderStatusCounts.getOrDefault("申请退货", 0L)
                ),
                "channel_orders", channelOrders,
                "inventory", aggregateInventorySummary(inventorySummary),
                "finance", Map.of(
                        "revenue", decimalValue(orderStats.get("revenue")),
                        "refunded", decimalValue(returnStats.get("refunded")),
                        "net_revenue", decimalValue(orderStats.get("revenue")).subtract(decimalValue(returnStats.get("refunded")))
                )
        );
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> dashboard() {
        List<Map<String, Object>> orders = fetchOrderRows(null, 1, 100);
        List<Map<String, Object>> returns = safeListReturns();
        List<Map<String, Object>> alerts = safeAlerts();
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> todayOrders = orders.stream()
                .filter(item -> {
                    LocalDateTime dt = parseCreatedAt(String.valueOf(item.get("created_at")));
                    return dt != null && dt.toLocalDate().equals(today);
                })
                .toList();
        long toShipCount = orders.stream()
                .filter(item -> {
                    String status = String.valueOf(item.get("status"));
                    return "已支付".equals(status) || "备货中".equals(status) || "已发货".equals(status);
                })
                .count();
        long returnRequests = returns.stream()
                .filter(item -> "待处理".equals(item.get("status")))
                .count();
        BigDecimal todayAmount = todayOrders.stream()
                .map(item -> decimalValue(item.get("total_amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Map.of(
                "today", Map.of(
                        "order_count", todayOrders.size(),
                        "amount", todayAmount
                ),
                "pending", Map.of(
                        "to_ship", toShipCount,
                        "return_requests", returnRequests
                ),
                "alerts", Map.of(
                        "low_stock", alerts.size()
                )
        );
    }

    public Map<String, Object> ordersByStatus(String status, int page, int limit) {
        return orderClient.listOrders(page, limit, null, status);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregateChannelOrders() {
        try {
            Map<String, Object> response = channelClient.listChannelOrders(1, 1000, null, null);
            List<Map<String, Object>> channelOrderList = response.get("data") == null
                    ? Collections.emptyList()
                    : (List<Map<String, Object>>) response.get("data");

            BigDecimal channelAmount = channelOrderList.stream()
                    .map(item -> decimalValue(item.get("total_amount")))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long channelPaidCount = channelOrderList.stream()
                    .filter(item -> "PAID".equals(item.get("unified_status")))
                    .count();
            long channelShippedCount = channelOrderList.stream()
                    .filter(item -> "SHIPPED".equals(item.get("unified_status")))
                    .count();
            long channelCompletedCount = channelOrderList.stream()
                    .filter(item -> "COMPLETED".equals(item.get("unified_status")))
                    .count();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", channelOrderList.size());
            result.put("total_amount", channelAmount);
            result.put("paid", channelPaidCount);
            result.put("shipped", channelShippedCount);
            result.put("completed", channelCompletedCount);
            return result;
        } catch (Exception e) {
            return Map.of("total", 0, "total_amount", BigDecimal.ZERO, "paid", 0, "shipped", 0, "completed", 0);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchOrderRows(String status, int page, int limit) {
        Map<String, Object> response = orderClient.listOrders(page, limit, null, status);
        Object data = response.get("data");
        return data == null ? Collections.emptyList() : (List<Map<String, Object>>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> aggregateInventorySummary(Map<String, Object> summary) {
        int total = 0;
        int locked = 0;
        BigDecimal stockValue = BigDecimal.ZERO;

        for (Object value : summary.values()) {
            Map<String, Object> row = (Map<String, Object>) value;
            total += intValue(row.get("total_qty"));
            locked += intValue(row.get("locked_qty"));
            stockValue = stockValue.add(decimalValue(row.get("stock_value")));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("total", total);
        payload.put("locked", locked);
        payload.put("available", total - locked);
        payload.put("stock_value", stockValue);
        return payload;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeListReturns() {
        try {
            Map<String, Object> response = returnClient.listReturns(1, 100, null);
            Object data = response.get("data");
            return data != null ? (List<Map<String, Object>>) data : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> safeSummary() {
        try {
            Map<String, Object> result = inventoryClient.summary();
            return result != null ? result : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> safeAlerts() {
        try {
            List<Map<String, Object>> result = inventoryClient.alerts();
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> safeProductStats() {
        try {
            Map<String, Object> result = productClient.getProductStats();
            return result != null ? result : Map.of("total", 0, "status_counts", Collections.emptyMap());
        } catch (Exception e) {
            return Map.of("total", 0, "status_counts", Collections.emptyMap());
        }
    }

    private Map<String, Object> safeOrderStats() {
        try {
            Map<String, Object> result = orderClient.getOrderStats();
            return result != null ? result : Map.of("total", 0, "status_counts", Collections.emptyMap(), "revenue", BigDecimal.ZERO);
        } catch (Exception e) {
            return Map.of("total", 0, "status_counts", Collections.emptyMap(), "revenue", BigDecimal.ZERO);
        }
    }

    private Map<String, Object> safeReturnStats() {
        try {
            Map<String, Object> result = returnClient.getReturnStats();
            return result != null ? result : Map.of("total", 0, "status_counts", Collections.emptyMap(), "refunded", BigDecimal.ZERO);
        } catch (Exception e) {
            return Map.of("total", 0, "status_counts", Collections.emptyMap(), "refunded", BigDecimal.ZERO);
        }
    }

    private int intValue(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseCreatedAt(String value) {
        if (value == null || "null".equals(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return null;
        }
    }
}
