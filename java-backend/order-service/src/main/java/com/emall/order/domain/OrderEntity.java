package com.emall.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String status;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false)
    private String receiverAddress;

    @Column(name = "tracking_no", nullable = false)
    private String trackingNo;

    @Column(nullable = false)
    private String carrier;

    @Column(name = "current_node", nullable = false)
    private String currentNode;

    @Column(name = "items_info", nullable = false)
    private String itemsInfo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    protected OrderEntity() {
    }

    public OrderEntity(String orderNo, BigDecimal totalAmount, String status, String receiverName,
            String receiverPhone, String receiverAddress, String trackingNo, String carrier, String currentNode,
            String itemsInfo, Long productId, Integer quantity, String createdAt) {
        this.orderNo = orderNo;
        this.totalAmount = totalAmount;
        this.status = status;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.receiverAddress = receiverAddress;
        this.trackingNo = trackingNo;
        this.carrier = carrier;
        this.currentNode = currentNode;
        this.itemsInfo = itemsInfo;
        this.productId = productId;
        this.quantity = quantity;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public String getTrackingNo() {
        return trackingNo;
    }

    public String getCarrier() {
        return carrier;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public String getItemsInfo() {
        return itemsInfo;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public void setTrackingNo(String trackingNo) {
        this.trackingNo = trackingNo;
    }
}
