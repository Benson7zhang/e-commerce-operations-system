package com.emall.channel.adapter;

import com.emall.channel.adapter.model.PageResult;
import com.emall.channel.adapter.model.PlatformInventory;
import com.emall.channel.adapter.model.PlatformOrder;
import com.emall.channel.adapter.model.PlatformProduct;
import com.emall.channel.adapter.model.PlatformReturn;
import com.emall.channel.adapter.model.PlatformShipment;
import java.time.Instant;
import java.util.List;

public interface ChannelAdapter {

    // ---- Identity ----

    String channelCode();

    String channelName();

    boolean supports(String channelCode);

    // ---- Lifecycle ----

    default void initialize(ChannelAdapterConfig config) {
    }

    default void destroy() {
    }

    boolean testConnection();

    // ---- Orders ----

    PageResult<PlatformOrder> fetchOrders(Instant startTime, Instant endTime, int page, int pageSize);

    PlatformOrder fetchOrderDetail(String platformOrderId);

    // ---- Products ----

    default PageResult<PlatformProduct> fetchProducts(int page, int pageSize) {
        return new PageResult<>(List.of(), 0, false);
    }

    default PlatformProduct fetchProductDetail(String platformSkuId) {
        return null;
    }

    // ---- Inventory ----

    default List<PlatformInventory> fetchInventory(List<String> skuIds) {
        return List.of();
    }

    // ---- Returns ----

    default PageResult<PlatformReturn> fetchReturns(Instant startTime, Instant endTime, int page, int pageSize) {
        return new PageResult<>(List.of(), 0, false);
    }

    // ---- Shipping ----

    default void pushShipment(PlatformShipment shipment) {
    }
}
