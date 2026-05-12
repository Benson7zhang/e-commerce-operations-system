package com.emall.inventory.web;

import com.emall.inventory.domain.InventoryEntity;
import com.emall.inventory.service.InventoryService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/alerts")
    public List<Map<String, Object>> alerts() {
        return inventoryService.alerts();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        return inventoryService.summary();
    }

    @GetMapping("/products")
    public List<Map<String, Object>> listProductsInventory() {
        return inventoryService.list().stream().map(this::toView).toList();
    }

    @GetMapping("/products/{productId}")
    public Map<String, Object> getProductInventory(@PathVariable long productId) {
        return toView(inventoryService.requireByProductId(productId));
    }

    @PostMapping("/products")
    public Map<String, Object> createOrUpdate(@Valid @RequestBody CreateInventoryRequest request) {
        InventoryEntity entity = inventoryService.upsert(
                request.productId(),
                request.productName(),
                request.warehouseId(),
                request.warehouseName(),
                request.quantity(),
                request.lockedQty(),
                request.alertThreshold(),
                request.unitCost()
        );
        return toView(entity);
    }

    @PutMapping("/products/{productId}/set")
    public Map<String, Object> setInventory(@PathVariable long productId, @Valid @RequestBody SetInventoryRequest request) {
        InventoryEntity entity = inventoryService.setQuantity(productId, request.quantity(), request.lockedQty());
        return toView(entity);
    }

    @PutMapping("/products/{productId}/increase")
    public Map<String, Object> increase(@PathVariable long productId, @Valid @RequestBody AdjustInventoryRequest request) {
        InventoryEntity entity = inventoryService.increase(productId, request.quantity());
        return toView(entity);
    }

    @PutMapping("/products/{productId}/decrease")
    public Map<String, Object> decrease(@PathVariable long productId, @Valid @RequestBody AdjustInventoryRequest request) {
        InventoryEntity entity = inventoryService.decrease(productId, request.quantity());
        return toView(entity);
    }

    @DeleteMapping("/products/{productId}")
    public Map<String, Object> delete(@PathVariable long productId) {
        inventoryService.deleteByProductId(productId);
        return Map.of("message", "deleted");
    }

    private Map<String, Object> toView(InventoryEntity entity) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", entity.getId());
        row.put("product_id", entity.getProductId());
        row.put("product_name", entity.getProductName());
        row.put("warehouse_id", entity.getWarehouseId());
        row.put("warehouse_name", entity.getWarehouseName());
        row.put("quantity", entity.getQuantity());
        row.put("locked_qty", entity.getLockedQty());
        row.put("available", entity.getQuantity() - entity.getLockedQty());
        row.put("alert_threshold", entity.getAlertThreshold());
        row.put("unit_cost", entity.getUnitCost());
        return row;
    }

    public record AdjustInventoryRequest(int quantity) {
    }

    public record SetInventoryRequest(int quantity, @JsonProperty("locked_qty") Integer lockedQty) {
    }

    public record CreateInventoryRequest(
            @JsonProperty("product_id") Long productId,
            @JsonProperty("product_name") String productName,
            @JsonProperty("warehouse_id") Long warehouseId,
            @JsonProperty("warehouse_name") String warehouseName,
            int quantity,
            @JsonProperty("locked_qty") Integer lockedQty,
            @JsonProperty("alert_threshold") Integer alertThreshold,
            @JsonProperty("unit_cost") BigDecimal unitCost
    ) {
    }
}
