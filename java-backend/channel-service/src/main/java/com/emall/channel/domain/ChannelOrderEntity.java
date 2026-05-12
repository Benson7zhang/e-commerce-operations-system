package com.emall.channel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "channel_orders", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"channel_code", "platform_order_id"})
})
public class ChannelOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "platform_order_id", nullable = false)
    private String platformOrderId;

    @Column(name = "unified_status", nullable = false)
    private String unifiedStatus;

    @Column(name = "platform_status", nullable = false)
    private String platformStatus;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "buyer_name", nullable = false)
    private String buyerName;

    @Column(name = "buyer_phone", nullable = false)
    private String buyerPhone;

    @Column(name = "buyer_address", nullable = false)
    private String buyerAddress;

    @Column(name = "items_summary", nullable = false)
    private String itemsSummary;

    @Column(nullable = false)
    private String carrier;

    @Column(name = "tracking_no", nullable = false)
    private String trackingNo;

    @Column(name = "order_time")
    private Instant orderTime;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Column(name = "raw_data_json", length = 4096)
    private String rawDataJson;

    protected ChannelOrderEntity() {
    }

    public ChannelOrderEntity(Long id, String channelCode, String platformOrderId, String unifiedStatus,
            String platformStatus, BigDecimal totalAmount, String buyerName, String buyerPhone, String buyerAddress,
            String itemsSummary, String carrier, String trackingNo, Instant orderTime, Instant syncedAt,
            String rawDataJson) {
        this.id = id;
        this.channelCode = channelCode;
        this.platformOrderId = platformOrderId;
        this.unifiedStatus = unifiedStatus;
        this.platformStatus = platformStatus;
        this.totalAmount = totalAmount;
        this.buyerName = buyerName;
        this.buyerPhone = buyerPhone;
        this.buyerAddress = buyerAddress;
        this.itemsSummary = itemsSummary;
        this.carrier = carrier;
        this.trackingNo = trackingNo;
        this.orderTime = orderTime;
        this.syncedAt = syncedAt;
        this.rawDataJson = rawDataJson;
    }

    public Long getId() {
        return id;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public String getPlatformOrderId() {
        return platformOrderId;
    }

    public String getUnifiedStatus() {
        return unifiedStatus;
    }

    public String getPlatformStatus() {
        return platformStatus;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerPhone() {
        return buyerPhone;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public String getItemsSummary() {
        return itemsSummary;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public Instant getOrderTime() {
        return orderTime;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public String getRawDataJson() {
        return rawDataJson;
    }
}
