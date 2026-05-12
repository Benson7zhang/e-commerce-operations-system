package com.emall.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(name = "product_type", nullable = false)
    private String type;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "cost_price", nullable = false)
    private BigDecimal costPrice;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int locked;

    protected ProductEntity() {
    }

    public ProductEntity(String sku, String name, String type, BigDecimal unitPrice, BigDecimal costPrice,
            String status, int stock, int locked) {
        this.sku = sku;
        this.name = name;
        this.type = type;
        this.unitPrice = unitPrice;
        this.costPrice = costPrice;
        this.status = status;
        this.stock = stock;
        this.locked = locked;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public String getStatus() {
        return status;
    }

    public int getStock() {
        return stock;
    }

    public int getLocked() {
        return locked;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public void setLocked(int locked) {
        this.locked = locked;
    }
}
