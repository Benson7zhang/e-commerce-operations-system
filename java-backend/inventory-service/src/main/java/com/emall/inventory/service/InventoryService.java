package com.emall.inventory.service;

import com.emall.common.config.RabbitMqConfig;
import com.emall.common.config.RedisService;
import com.emall.common.mq.event.InventoryChangedEvent;
import com.emall.inventory.domain.InventoryEntity;
import com.emall.inventory.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private static final String CACHE_PREFIX = "inventory:product:";
    private static final long CACHE_TTL_MINUTES = 10;

    private final InventoryRepository repository;
    private final RedisService redisService;
    private final RabbitTemplate rabbitTemplate;

    public InventoryService(InventoryRepository repository,
            @Autowired(required = false) RedisService redisService,
            @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.redisService = redisService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<InventoryEntity> list() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(InventoryEntity::getProductId))
                .toList();
    }

    public List<Map<String, Object>> alerts() {
        return list().stream()
                .filter(item -> item.getQuantity() <= item.getAlertThreshold())
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("product_id", item.getProductId());
                    row.put("product_name", item.getProductName());
                    row.put("available", item.getQuantity() - item.getLockedQty());
                    row.put("threshold", item.getAlertThreshold());
                    return row;
                })
                .toList();
    }

    public Map<String, Object> summary() {
        List<InventoryEntity> rows = list();
        Map<String, Map<String, Object>> summary = new LinkedHashMap<>();

        for (InventoryEntity item : rows) {
            Map<String, Object> bucket = summary.computeIfAbsent(item.getWarehouseName(), key -> {
                Map<String, Object> initial = new LinkedHashMap<>();
                initial.put("total_sku", 0);
                initial.put("total_qty", 0);
                initial.put("locked_qty", 0);
                initial.put("stock_value", BigDecimal.ZERO);
                return initial;
            });

            bucket.put("total_sku", ((Integer) bucket.get("total_sku")) + 1);
            bucket.put("total_qty", ((Integer) bucket.get("total_qty")) + item.getQuantity());
            bucket.put("locked_qty", ((Integer) bucket.get("locked_qty")) + item.getLockedQty());
            bucket.put(
                    "stock_value",
                    ((BigDecimal) bucket.get("stock_value")).add(item.getUnitCost().multiply(BigDecimal.valueOf(item.getQuantity())))
            );
        }

        return new LinkedHashMap<>(summary);
    }

    @Transactional
    public InventoryEntity upsert(long productId, String productName, long warehouseId, String warehouseName, int quantity,
            Integer lockedQty, Integer alertThreshold, BigDecimal unitCost) {
        InventoryEntity entity = repository.findByProductId(productId)
                .orElseGet(() -> new InventoryEntity(
                        productId,
                        productName,
                        warehouseId,
                        warehouseName,
                        quantity,
                        lockedQty == null ? 0 : lockedQty,
                        alertThreshold == null ? 10 : alertThreshold,
                        unitCost
                ));

        entity.setProductName(productName);
        entity.setWarehouseId(warehouseId);
        entity.setWarehouseName(warehouseName);
        entity.setQuantity(quantity);
        entity.setLockedQty(lockedQty == null ? entity.getLockedQty() : lockedQty);
        entity.setAlertThreshold(alertThreshold == null ? entity.getAlertThreshold() : alertThreshold);
        entity.setUnitCost(unitCost);
        InventoryEntity saved = repository.save(entity);
        evictInventoryCache(productId);
        return saved;
    }

    public InventoryEntity requireByProductId(long productId) {
        InventoryEntity entity = repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));
        if (redisService != null) {
            try {
                redisService.set(CACHE_PREFIX + productId, entity, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.debug("Failed to cache inventory for product {}: {}", productId, e.getMessage());
            }
        }
        return entity;
    }

    @Transactional
    public InventoryEntity setQuantity(long productId, int quantity, Integer lockedQty) {
        InventoryEntity entity = requireByProductId(productId);
        entity.setQuantity(quantity);
        if (lockedQty != null) {
            entity.setLockedQty(lockedQty);
        }
        InventoryEntity saved = repository.save(entity);
        evictInventoryCache(productId);
        return saved;
    }

    @Transactional
    public InventoryEntity increase(long productId, int quantity) {
        InventoryEntity entity = requireByProductId(productId);
        int beforeQty = entity.getQuantity();
        entity.setQuantity(beforeQty + quantity);
        InventoryEntity saved = repository.save(entity);

        publishInventoryChangedEvent(productId, InventoryChangedEvent.ChangeType.INCREASE,
                quantity, beforeQty, saved.getQuantity(), null, null);

        evictInventoryCache(productId);
        return saved;
    }

    @Transactional
    public InventoryEntity decrease(long productId, int quantity) {
        int affected = repository.decreaseQuantity(productId, quantity);
        if (affected == 0) {
            throw new IllegalStateException("库存不足");
        }
        InventoryEntity entity = repository.findByProductId(productId)
                .orElseThrow(() -> new IllegalArgumentException("库存记录不存在"));

        publishInventoryChangedEvent(productId, InventoryChangedEvent.ChangeType.DECREASE,
                quantity, entity.getQuantity() + quantity, entity.getQuantity(), null, null);

        evictInventoryCache(productId);
        return entity;
    }

    @Transactional
    public void deleteByProductId(long productId) {
        requireByProductId(productId);
        repository.deleteByProductId(productId);
        evictInventoryCache(productId);
    }

    private void publishInventoryChangedEvent(long productId, InventoryChangedEvent.ChangeType changeType,
            int quantity, int beforeQty, int afterQty, String refType, String refId) {
        if (rabbitTemplate == null) {
            return;
        }
        try {
            InventoryChangedEvent event = new InventoryChangedEvent(
                    productId, changeType, quantity, beforeQty, afterQty, refType, refId);
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE_NAME,
                    "inventory." + changeType.name().toLowerCase(),
                    event
            );
            log.info("InventoryChangedEvent published: productId={}, changeType={}, qty={}",
                    productId, changeType, quantity);
        } catch (Exception e) {
            log.error("Failed to publish InventoryChangedEvent for productId={}, changeType={}: {}",
                    productId, changeType, e.getMessage(), e);
        }
    }

    private void evictInventoryCache(long productId) {
        if (redisService != null) {
            try {
                redisService.delete(CACHE_PREFIX + productId);
            } catch (Exception e) {
                log.debug("Failed to evict inventory cache for product {}: {}", productId, e.getMessage());
            }
        }
    }
}
