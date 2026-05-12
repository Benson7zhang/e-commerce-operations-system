package com.emall.returnservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "returns")
public class ReturnEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "receiver_name", nullable = false)
    private String receiverName;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status;

    @Column(name = "refund_amount")
    private BigDecimal refundAmount;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    protected ReturnEntity() {
    }

    public ReturnEntity(Long orderId, String orderNo, String receiverName, BigDecimal totalAmount,
            String reason, String status, BigDecimal refundAmount, String rejectReason, String createdAt,
            Long productId, Integer quantity) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.receiverName = receiverName;
        this.totalAmount = totalAmount;
        this.reason = reason;
        this.status = status;
        this.refundAmount = refundAmount;
        this.rejectReason = rejectReason;
        this.createdAt = createdAt;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getReason() {
        return reason;
    }

    public String getStatus() {
        return status;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
