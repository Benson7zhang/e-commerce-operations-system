package com.emall.product.web;

import com.emall.product.domain.ProductEntity;
import com.emall.product.service.ProductService;
import com.emall.product.service.ProductService.InventorySnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Map<String, Object> listProducts(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "type_filter", required = false) String typeFilter,
            @RequestParam(name = "status", required = false) String status
    ) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("id").descending());
        Page<ProductEntity> result = productService.listPaged(search, typeFilter, status, pageable);
        Map<Long, InventorySnapshot> inventoryByProductId = productService.inventorySnapshots();

        return Map.of(
                "data", result.getContent().stream()
                        .map(item -> toSummary(item, inventoryByProductId.get(item.getId())))
                        .toList(),
                "pagination", Map.of(
                        "page", page,
                        "limit", limit,
                        "total", result.getTotalElements(),
                        "pages", result.getTotalPages()
                )
        );
    }

    @GetMapping("/types")
    public List<String> types() {
        return productService.types();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return productService.productStats();
    }

    @PostMapping
    public Map<String, Object> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductEntity entity = productService.create(
                request.sku(),
                request.name(),
                request.type(),
                request.unitPrice(),
                request.costPrice(),
                request.stock(),
                request.warehouseId()
        );
        return Map.of("id", entity.getId(), "message", "created");
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateProduct(@PathVariable long id, @Valid @RequestBody ProductUpdateRequest request) {
        productService.update(id, request.name(), request.type(), request.unitPrice(), request.costPrice());
        return Map.of("message", "updated");
    }

    @PutMapping("/{id}/toggle")
    public Map<String, Object> toggleProduct(@PathVariable long id) {
        ProductEntity entity = productService.toggle(id);
        return Map.of("message", "toggled", "status", entity.getStatus());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deleteProduct(@PathVariable long id) {
        productService.delete(id);
        return Map.of("message", "deleted");
    }

    @GetMapping("/{id}")
    public Map<String, Object> getProduct(@PathVariable long id) {
        ProductEntity item = productService.require(id);
        return toSummary(item, productService.inventorySnapshot(id));
    }

    @GetMapping("/{id}/inventory")
    public Map<String, Object> getInventory(@PathVariable long id) {
        InventorySnapshot current = productService.inventorySnapshot(id);
        return Map.of(
                "inventory", List.of(
                        Map.of(
                                "warehouse_id", current.warehouseId(),
                                "warehouse_name", current.warehouseName(),
                                "quantity", current.quantity(),
                                "locked_qty", current.lockedQty(),
                                "available", current.available()
                        )
                )
        );
    }

    @PutMapping("/{id}/inventory")
    public Map<String, Object> updateInventory(
            @PathVariable long id,
            @RequestParam(name = "warehouse_id") long warehouseId,
            @RequestParam(name = "quantity") int quantity
    ) {
        InventorySnapshot snapshot = productService.updateInventory(id, warehouseId, quantity, null);
        return Map.of(
                "message", "inventory_updated",
                "warehouse_id", snapshot.warehouseId(),
                "quantity", snapshot.quantity(),
                "available", snapshot.available()
        );
    }

    @PutMapping("/{id}/inventory/decrease")
    public Map<String, Object> decreaseInventory(@PathVariable long id, @Valid @RequestBody InventoryAdjustRequest request) {
        InventorySnapshot updated = productService.decreaseInventory(id, request.quantity());
        return Map.of("message", "inventory_decreased", "available", updated.available());
    }

    @PutMapping("/{id}/inventory/increase")
    public Map<String, Object> increaseInventory(@PathVariable long id, @Valid @RequestBody InventoryAdjustRequest request) {
        InventorySnapshot updated = productService.increaseInventory(id, request.quantity());
        return Map.of("message", "inventory_increased", "available", updated.available());
    }

    private Map<String, Object> toSummary(ProductEntity item, InventorySnapshot inventory) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", item.getId());
        row.put("sku", item.getSku());
        row.put("name", item.getName());
        row.put("type", item.getType());
        row.put("unit_price", item.getUnitPrice());
        row.put("cost_price", item.getCostPrice());
        row.put("profit", item.getUnitPrice().subtract(item.getCostPrice()).setScale(2, RoundingMode.HALF_UP));
        row.put("stock", inventory == null ? item.getStock() : inventory.quantity());
        row.put("locked", inventory == null ? item.getLocked() : inventory.lockedQty());
        row.put("available", inventory == null ? item.getStock() - item.getLocked() : inventory.available());
        row.put("status", item.getStatus());
        return row;
    }

    public record ProductCreateRequest(
            String sku,
            String name,
            String type,
            BigDecimal unitPrice,
            BigDecimal costPrice,
            int stock,
            Long warehouseId
    ) {
    }

    public record ProductUpdateRequest(
            String name,
            String type,
            BigDecimal unitPrice,
            BigDecimal costPrice
    ) {
    }

    public record InventoryAdjustRequest(int quantity) {
    }
}
