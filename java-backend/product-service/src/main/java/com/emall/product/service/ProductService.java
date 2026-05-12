package com.emall.product.service;

import com.emall.common.config.RedisService;
import com.emall.common.feign.InventoryFeignClient;
import com.emall.common.feign.dto.QuantityRequest;
import com.emall.common.feign.dto.SetInventoryRequest;
import com.emall.common.feign.dto.UpsertInventoryRequest;
import com.emall.common.saga.SagaContext;
import com.emall.common.saga.SagaOrchestrator;
import com.emall.common.saga.SagaStep;
import com.emall.product.domain.ProductEntity;
import com.emall.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private static final String CACHE_PREFIX = "product:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final ProductRepository repository;
    private final InventoryFeignClient inventoryClient;
    private final RedisService redisService;

    public ProductService(ProductRepository repository, InventoryFeignClient inventoryClient,
            @Autowired(required = false) RedisService redisService) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
        this.redisService = redisService;
    }

    public List<ProductEntity> list(String search, String typeFilter, String status) {
        List<ProductEntity> candidates;
        if (search != null && !search.isBlank()) {
            candidates = repository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search, search);
        } else if (status != null && !status.isBlank()) {
            candidates = repository.findByStatus(status);
        } else if (typeFilter != null && !typeFilter.isBlank()) {
            candidates = repository.findByType(typeFilter);
        } else {
            candidates = repository.findAll();
        }

        return candidates.stream()
                .filter(item -> typeFilter == null || typeFilter.isBlank() || Objects.equals(item.getType(), typeFilter))
                .filter(item -> status == null || status.isBlank() || Objects.equals(item.getStatus(), status))
                .sorted(Comparator.comparing(ProductEntity::getId).reversed())
                .toList();
    }

    public Page<ProductEntity> listPaged(String search, String typeFilter, String status, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search;
        String normalizedType = (typeFilter == null || typeFilter.isBlank()) ? null : typeFilter;
        String normalizedStatus = (status == null || status.isBlank()) ? null : status;
        return repository.findByFilters(normalizedSearch, normalizedType, normalizedStatus, pageable);
    }

    public List<String> types() {
        return repository.findAll().stream()
                .map(ProductEntity::getType)
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Object> productStats() {
        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        for (Object[] row : repository.countByStatus()) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }
        return Map.of(
                "total", repository.count(),
                "status_counts", statusCounts
        );
    }

    @Transactional
    public ProductEntity create(String sku, String name, String type, BigDecimal unitPrice, BigDecimal costPrice, int stock,
            Long warehouseId) {
        ProductEntity entity = new ProductEntity(
                sku,
                name,
                type,
                unitPrice,
                costPrice,
                "在售",
                stock,
                0
        );
        long inventoryWarehouseId = warehouseId == null ? 1L : warehouseId;

        new SagaOrchestrator("create-product")
                .addStep(new SagaStep(
                        "persist-product",
                        sagaContext -> {
                            repository.save(entity);
                            return sagaContext;
                        },
                        sagaContext -> repository.deleteById(entity.getId())
                ))
                .addStep(new SagaStep(
                        "create-inventory",
                        sagaContext -> {
                            inventoryClient.upsertInventory(new UpsertInventoryRequest(
                                    entity.getId(),
                                    entity.getName(),
                                    inventoryWarehouseId,
                                    warehouseNameOf(inventoryWarehouseId),
                                    stock,
                                    entity.getLocked(),
                                    10,
                                    costPrice
                            ));
                            return sagaContext;
                        },
                        sagaContext -> inventoryClient.deleteInventory(entity.getId())
                ))
                .execute(new SagaContext().put("productId", entity.getId()));
        return entity;
    }

    @Transactional
    public ProductEntity update(long id, String name, String type, BigDecimal unitPrice, BigDecimal costPrice) {
        ProductEntity entity = require(id);
        if (name != null) {
            entity.setName(name);
        }
        if (type != null) {
            entity.setType(type);
        }
        if (unitPrice != null) {
            entity.setUnitPrice(unitPrice);
        }
        if (costPrice != null) {
            entity.setCostPrice(costPrice);
        }
        ProductEntity saved = repository.save(entity);
        syncInventoryMetadata(saved);
        evictProductCache(id);
        return saved;
    }

    @Transactional
    public ProductEntity toggle(long id) {
        ProductEntity entity = require(id);
        entity.setStatus("在售".equals(entity.getStatus()) ? "下架" : "在售");
        ProductEntity saved = repository.save(entity);
        evictProductCache(id);
        return saved;
    }

    @Transactional
    public void delete(long id) {
        repository.deleteById(id);
        inventoryClient.deleteInventory(id);
        evictProductCache(id);
    }

    @Transactional
    public ProductEntity updateStock(long id, int quantity) {
        ProductEntity entity = require(id);
        entity.setStock(quantity);
        return repository.save(entity);
    }

    @Transactional
    public ProductEntity decreaseStock(long id, int quantity) {
        ProductEntity entity = require(id);
        if (entity.getStock() - entity.getLocked() < quantity) {
            throw new IllegalStateException("库存不足");
        }
        entity.setStock(entity.getStock() - quantity);
        return repository.save(entity);
    }

    @Transactional
    public ProductEntity increaseStock(long id, int quantity) {
        ProductEntity entity = require(id);
        entity.setStock(entity.getStock() + quantity);
        return repository.save(entity);
    }

    public Map<Long, InventorySnapshot> inventorySnapshots() {
        return inventoryClient.listInventory().stream()
                .map(this::toSnapshot)
                .collect(java.util.stream.Collectors.toMap(
                        InventorySnapshot::productId,
                        snapshot -> snapshot,
                        (left, right) -> right
                ));
    }

    public InventorySnapshot inventorySnapshot(long productId) {
        return toSnapshot(inventoryClient.getInventory(productId));
    }

    public InventorySnapshot updateInventory(long productId, int quantity, Integer lockedQty) {
        InventorySnapshot snapshot = toSnapshot(inventoryClient.setInventory(productId, new SetInventoryRequest(quantity, lockedQty)));
        ProductEntity entity = require(productId);
        entity.setStock(snapshot.quantity());
        entity.setLocked(snapshot.lockedQty());
        repository.save(entity);
        return snapshot;
    }

    public InventorySnapshot increaseInventory(long productId, int quantity) {
        InventorySnapshot snapshot = toSnapshot(inventoryClient.increaseInventory(productId, new QuantityRequest(quantity)));
        syncMirrorFields(productId, snapshot);
        return snapshot;
    }

    public InventorySnapshot decreaseInventory(long productId, int quantity) {
        InventorySnapshot snapshot = toSnapshot(inventoryClient.decreaseInventory(productId, new QuantityRequest(quantity)));
        syncMirrorFields(productId, snapshot);
        return snapshot;
    }

    public InventorySnapshot updateInventory(long productId, long warehouseId, int quantity, Integer lockedQty) {
        ProductEntity entity = require(productId);
        InventorySnapshot current = inventorySnapshot(productId);
        InventorySnapshot snapshot = toSnapshot(inventoryClient.upsertInventory(new UpsertInventoryRequest(
                productId,
                entity.getName(),
                warehouseId,
                warehouseNameOf(warehouseId),
                quantity,
                lockedQty == null ? current.lockedQty() : lockedQty,
                current.alertThreshold(),
                entity.getCostPrice()
        )));
        syncMirrorFields(productId, snapshot);
        return snapshot;
    }

    public ProductEntity require(long id) {
        if (redisService != null) {
            try {
                Object cached = redisService.get(CACHE_PREFIX + id);
                if (cached instanceof ProductEntity entity) {
                    return entity;
                }
            } catch (Exception e) {
                log.debug("Cache miss for product {}: {}", id, e.getMessage());
            }
        }
        ProductEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (redisService != null) {
            redisService.set(CACHE_PREFIX + id, entity, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        }
        return entity;
    }

    private void evictProductCache(long id) {
        if (redisService != null) {
            try {
                redisService.delete(CACHE_PREFIX + id);
            } catch (Exception e) {
                log.debug("Failed to evict product cache for {}: {}", id, e.getMessage());
            }
        }
    }

    private void syncInventoryMetadata(ProductEntity entity) {
        InventorySnapshot current = inventorySnapshot(entity.getId());
        inventoryClient.upsertInventory(new UpsertInventoryRequest(
                entity.getId(),
                entity.getName(),
                current.warehouseId(),
                current.warehouseName(),
                current.quantity(),
                current.lockedQty(),
                current.alertThreshold(),
                entity.getCostPrice()
        ));
        syncMirrorFields(entity.getId(), inventorySnapshot(entity.getId()));
    }

    private void syncMirrorFields(long productId, InventorySnapshot snapshot) {
        ProductEntity entity = require(productId);
        entity.setStock(snapshot.quantity());
        entity.setLocked(snapshot.lockedQty());
        repository.save(entity);
    }

    private InventorySnapshot toSnapshot(Map<String, Object> payload) {
        return new InventorySnapshot(
                longValue(payload.get("product_id")),
                longValue(payload.get("warehouse_id")),
                String.valueOf(payload.get("warehouse_name")),
                intValue(payload.get("quantity")),
                intValue(payload.get("locked_qty")),
                intValue(payload.get("available")),
                intValue(payload.get("alert_threshold")),
                new BigDecimal(String.valueOf(payload.get("unit_cost")))
        );
    }

    private long longValue(Object value) {
        return ((Number) value).longValue();
    }

    private int intValue(Object value) {
        return ((Number) value).intValue();
    }

    private String warehouseNameOf(long warehouseId) {
        return switch ((int) warehouseId) {
            case 2 -> "华南仓";
            case 3 -> "华北仓";
            default -> "华东仓";
        };
    }

    public record InventorySnapshot(
            long productId,
            long warehouseId,
            String warehouseName,
            int quantity,
            int lockedQty,
            int available,
            int alertThreshold,
            BigDecimal unitCost
    ) {
    }
}
