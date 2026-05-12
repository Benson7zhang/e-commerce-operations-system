package com.emall.warehouse.web;

import com.emall.warehouse.domain.WarehouseEntity;
import com.emall.warehouse.service.WarehouseService;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public List<Map<String, Object>> listWarehouses() {
        return warehouseService.list().stream().map(this::toView).toList();
    }

    @GetMapping("/{id}")
    public Map<String, Object> getWarehouse(@PathVariable long id) {
        return toView(warehouseService.require(id));
    }

    private Map<String, Object> toView(WarehouseEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("name", entity.getName());
        row.put("region", entity.getRegion());
        row.put("status", entity.getStatus());
        row.put("address", entity.getAddress());
        return row;
    }
}
