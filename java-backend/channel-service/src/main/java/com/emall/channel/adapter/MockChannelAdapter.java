package com.emall.channel.adapter;

import com.emall.channel.adapter.model.PageResult;
import com.emall.channel.adapter.model.PlatformInventory;
import com.emall.channel.adapter.model.PlatformOrder;
import com.emall.channel.adapter.model.PlatformOrder.BuyerInfo;
import com.emall.channel.adapter.model.PlatformOrder.OrderItem;
import com.emall.channel.adapter.model.PlatformOrder.ShippingInfo;
import com.emall.channel.adapter.model.PlatformProduct;
import com.emall.channel.adapter.model.PlatformReturn;
import com.emall.channel.adapter.model.PlatformShipment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MockChannelAdapter implements ChannelAdapter {

    private static final Logger log = LoggerFactory.getLogger(MockChannelAdapter.class);

    @Override
    public String channelCode() {
        return "mock";
    }

    @Override
    public String channelName() {
        return "模拟渠道";
    }

    @Override
    public boolean supports(String channelCode) {
        return "mock".equals(channelCode);
    }

    @Override
    public void initialize(ChannelAdapterConfig config) {
        log.info("Mock adapter initialized with config: {}", config.properties());
    }

    @Override
    public void destroy() {
        log.info("Mock adapter destroyed");
    }

    @Override
    public boolean testConnection() {
        return true;
    }

    // ---- Orders ----

    @Override
    public PageResult<PlatformOrder> fetchOrders(Instant startTime, Instant endTime, int page, int pageSize) {
        List<PlatformOrder> orders = buildMockOrders();
        return new PageResult<>(orders, 1, false);
    }

    @Override
    public PlatformOrder fetchOrderDetail(String platformOrderId) {
        return buildMockOrders().stream()
                .filter(o -> o.platformOrderId().equals(platformOrderId))
                .findFirst()
                .orElse(null);
    }

    // ---- Products ----

    @Override
    public PageResult<PlatformProduct> fetchProducts(int page, int pageSize) {
        return new PageResult<>(buildMockProducts(), 1, false);
    }

    @Override
    public PlatformProduct fetchProductDetail(String platformSkuId) {
        return buildMockProducts().stream()
                .filter(p -> p.platformSkuId().equals(platformSkuId))
                .findFirst()
                .orElse(null);
    }

    // ---- Inventory ----

    @Override
    public List<PlatformInventory> fetchInventory(List<String> skuIds) {
        return List.of(
                new PlatformInventory("SKU-A001", "mock", 500, 20, 480),
                new PlatformInventory("SKU-B002", "mock", 200, 5, 195),
                new PlatformInventory("SKU-B003", "mock", 1000, 50, 950),
                new PlatformInventory("SKU-C004", "mock", 3000, 100, 2900),
                new PlatformInventory("SKU-D005", "mock", 50, 2, 48)
        );
    }

    // ---- Returns ----

    @Override
    public PageResult<PlatformReturn> fetchReturns(Instant startTime, Instant endTime, int page, int pageSize) {
        return new PageResult<>(buildMockReturns(), 1, false);
    }

    // ---- Shipping ----

    @Override
    public void pushShipment(PlatformShipment shipment) {
        log.info("Mock: pushed shipment for order {} - {} / {}",
                shipment.platformOrderId(), shipment.carrier(), shipment.trackingNo());
    }

    // ---- Mock data builders ----

    private List<PlatformOrder> buildMockOrders() {
        Instant now = Instant.now();
        return List.of(
                new PlatformOrder(
                        "MOCK-20260001", "mock", "已付款", null,
                        new BigDecimal("299.00"),
                        List.of(new OrderItem("SKU-A001", "无线蓝牙耳机", 1, new BigDecimal("299.00"))),
                        new BuyerInfo("张三", "13800001111", "北京市朝阳区建国路88号"),
                        new ShippingInfo("", ""),
                        now.minus(2, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS), null,
                        "模拟订单1",
                        Map.of("source", "mock")
                ),
                new PlatformOrder(
                        "MOCK-20260002", "mock", "已发货", null,
                        new BigDecimal("1599.00"),
                        List.of(
                                new OrderItem("SKU-B002", "智能手表 Pro", 1, new BigDecimal("1299.00")),
                                new OrderItem("SKU-B003", "表带配件套装", 1, new BigDecimal("300.00"))
                        ),
                        new BuyerInfo("李四", "13800002222", "上海市浦东新区陆家嘴环路1000号"),
                        new ShippingInfo("顺丰速运", "SF1234567890"),
                        now.minus(1, ChronoUnit.DAYS), now.minus(20, ChronoUnit.HOURS), now.minus(6, ChronoUnit.HOURS),
                        "模拟订单2",
                        Map.of("source", "mock")
                ),
                new PlatformOrder(
                        "MOCK-20260003", "mock", "已完成", null,
                        new BigDecimal("59.90"),
                        List.of(new OrderItem("SKU-C004", "Type-C 数据线 2m", 2, new BigDecimal("29.95"))),
                        new BuyerInfo("王五", "13800003333", "深圳市南山区科技园南路66号"),
                        new ShippingInfo("中通快递", "ZT9876543210"),
                        now.minus(3, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS),
                        "模拟订单3",
                        Map.of("source", "mock")
                ),
                new PlatformOrder(
                        "MOCK-20260004", "mock", "已取消", null,
                        new BigDecimal("4999.00"),
                        List.of(new OrderItem("SKU-D005", "笔记本电脑支架", 1, new BigDecimal("4999.00"))),
                        new BuyerInfo("赵六", "13800004444", "杭州市西湖区文三路500号"),
                        new ShippingInfo("", ""),
                        now.minus(5, ChronoUnit.DAYS), null, null,
                        "模拟订单4",
                        Map.of("source", "mock")
                )
        );
    }

    private List<PlatformProduct> buildMockProducts() {
        return List.of(
                new PlatformProduct(
                        "SKU-A001", "mock", "无线蓝牙耳机",
                        new BigDecimal("299.00"), new BigDecimal("120.00"),
                        "数码配件", "在售", "", List.of(), "高品质无线蓝牙耳机",
                        Map.of("color", "黑色", "brand", "E-Mall"),
                        Map.of("source", "mock")
                ),
                new PlatformProduct(
                        "SKU-B002", "mock", "智能手表 Pro",
                        new BigDecimal("1299.00"), new BigDecimal("600.00"),
                        "智能穿戴", "在售", "", List.of(), "全功能智能手表",
                        Map.of("color", "银色", "brand", "E-Mall"),
                        Map.of("source", "mock")
                ),
                new PlatformProduct(
                        "SKU-C004", "mock", "Type-C 数据线 2m",
                        new BigDecimal("29.95"), new BigDecimal("8.00"),
                        "数码配件", "在售", "", List.of(), "快充数据线",
                        Map.of("length", "2m", "brand", "E-Mall"),
                        Map.of("source", "mock")
                )
        );
    }

    private List<PlatformReturn> buildMockReturns() {
        Instant now = Instant.now();
        return List.of(
                new PlatformReturn(
                        "RET-20260001", "MOCK-20260003", "mock",
                        "已完成", "商品质量问题",
                        new BigDecimal("59.90"), "ZT9876543210",
                        now.minus(2, ChronoUnit.DAYS), now.minus(1, ChronoUnit.DAYS),
                        Map.of("source", "mock")
                )
        );
    }
}
