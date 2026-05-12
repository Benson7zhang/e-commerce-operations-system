package com.emall.returnservice.service;

import com.emall.common.config.RabbitMqConfig;
import com.emall.common.feign.InventoryFeignClient;
import com.emall.common.feign.dto.QuantityRequest;
import com.emall.common.mq.event.ReturnCompletedEvent;
import com.emall.common.saga.SagaContext;
import com.emall.common.saga.SagaOrchestrator;
import com.emall.common.saga.SagaStep;
import com.emall.returnservice.domain.ReturnEntity;
import com.emall.returnservice.repository.ReturnRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReturnService {

    private static final Logger log = LoggerFactory.getLogger(ReturnService.class);
    private final ReturnRepository repository;
    private final InventoryFeignClient inventoryClient;
    private final RabbitTemplate rabbitTemplate;

    public ReturnService(ReturnRepository repository, InventoryFeignClient inventoryClient,
                         @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<ReturnEntity> list(String status) {
        if (status == null || status.isBlank()) {
            return repository.findAll();
        }
        return repository.findByStatus(status);
    }

    public Page<ReturnEntity> listPaged(String status, Pageable pageable) {
        String normalizedStatus = (status == null || status.isBlank()) ? null : status;
        return repository.findByFilters(normalizedStatus, pageable);
    }

    public Map<String, Object> returnStats() {
        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        for (Object[] row : repository.countByStatus()) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }
        return Map.of(
                "total", repository.count(),
                "status_counts", statusCounts,
                "refunded", repository.sumRefunded()
        );
    }

    public ReturnEntity require(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("退货记录不存在"));
    }

    @Transactional
    public ReturnEntity approve(long id, BigDecimal refundAmount) {
        ReturnEntity entity = require(id);
        if (!"待处理".equals(entity.getStatus())) {
            throw new IllegalStateException("仅待处理的退货单可执行批准操作，当前状态: " + entity.getStatus());
        }
        if (refundAmount != null && refundAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("退款金额不能为负数");
        }
        entity.setStatus("已批准");
        entity.setRefundAmount(refundAmount);
        entity.setRejectReason(null);
        return repository.save(entity);
    }

    @Transactional
    public ReturnEntity reject(long id, String rejectReason) {
        ReturnEntity entity = require(id);
        if (!"待处理".equals(entity.getStatus())) {
            throw new IllegalStateException("仅待处理的退货单可执行拒绝操作，当前状态: " + entity.getStatus());
        }
        entity.setStatus("已拒绝");
        entity.setRejectReason(rejectReason);
        return repository.save(entity);
    }

    @Transactional
    public ReturnEntity complete(long id) {
        ReturnEntity entity = require(id);
        if ("已完成".equals(entity.getStatus())) {
            return entity;
        }
        if (!"已批准".equals(entity.getStatus())) {
            throw new IllegalStateException("仅已批准的退货单可执行完成操作，当前状态: " + entity.getStatus());
        }

        SagaContext context = new SagaContext()
                .put("returnId", id)
                .put("productId", entity.getProductId())
                .put("quantity", entity.getQuantity());

        new SagaOrchestrator("complete-return")
                .addStep(new SagaStep(
                        "increase-inventory",
                        sagaContext -> {
                            inventoryClient.increaseInventory(entity.getProductId(), new QuantityRequest(entity.getQuantity()));
                            return sagaContext;
                        },
                        sagaContext -> inventoryClient.decreaseInventory(entity.getProductId(), new QuantityRequest(entity.getQuantity()))
                ))
                .addStep(new SagaStep(
                        "update-return-status",
                        sagaContext -> {
                            entity.setStatus("已完成");
                            repository.save(entity);
                            return sagaContext;
                        },
                        sagaContext -> {
                            entity.setStatus("已批准");
                            repository.save(entity);
                        }
                ))
                .execute(context);

        if (rabbitTemplate != null) {
            try {
                ReturnCompletedEvent event = new ReturnCompletedEvent(
                        entity.getId(),
                        entity.getOrderId(),
                        entity.getProductId(),
                        entity.getQuantity(),
                        entity.getRefundAmount(),
                        LocalDateTime.now()
                );
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE_NAME,
                        RabbitMqConfig.RETURN_COMPLETED_ROUTING_KEY,
                        event
                );
                log.info("ReturnCompletedEvent published for returnId={}", entity.getId());
            } catch (Exception e) {
                log.error("Failed to publish ReturnCompletedEvent for returnId={}: {}",
                        entity.getId(), e.getMessage(), e);
            }
        }

        return entity;
    }
}
