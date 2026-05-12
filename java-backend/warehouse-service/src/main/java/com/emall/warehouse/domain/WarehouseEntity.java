package com.emall.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse")
public class WarehouseEntity {

    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private Integer status;

    @Column(nullable = false)
    private String address;

    protected WarehouseEntity() {
    }

    public WarehouseEntity(Long id, String name, String region, Integer status, String address) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.status = status;
        this.address = address;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public Integer getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }
}
