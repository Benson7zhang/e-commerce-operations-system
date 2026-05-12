package com.emall.order.service;

import com.emall.common.config.RabbitMqConfig;
import com.emall.common.feign.InventoryFeignClient;
import com.emall.common.feign.ProductFeignClient;
import com.emall.common.feign.dto.QuantityRequest;
import com.emall.common.mq.event.OrderCreatedEvent;
import com.emall.common.saga.SagaContext;
import com.emall.common.saga.SagaOrchestrator;
import com.emall.common.saga.SagaStep;
import com.emall.order.domain.OrderEntity;
import com.emall.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final OrderRepository repository;
    private final InventoryFeignClient inventoryClient;
    private final ProductFeignClient productClient;
    private final RabbitTemplate rabbitTemplate;

    public OrderService(OrderRepository repository, InventoryFeignClient inventoryClient,
                        ProductFeignClient productClient,
                        @Autowired(required = false) RabbitTemplate rabbitTemplate) {
        this.repository = repository;
        this.inventoryClient = inventoryClient;
        this.productClient = productClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    public List<OrderEntity> list(String search, String status) {
        List<OrderEntity> candidates;
        if (search != null && !search.isBlank()) {
            candidates = repository.findByOrderNoContainingIgnoreCaseOrReceiverNameContainingIgnoreCaseOrReceiverPhoneContainingIgnoreCase(
                    search, search, search);
        } else if (status != null && !status.isBlank()) {
            candidates = repository.findByStatus(status);
        } else {
            candidates = repository.findAll();
        }

        return candidates.stream()
                .filter(item -> status == null || status.isBlank() || Objects.equals(item.getStatus(), status))
                .sorted(Comparator.comparing(OrderEntity::getId).reversed())
                .toList();
    }

    public Page<OrderEntity> listPaged(String search, String status, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search;
        String normalizedStatus = (status == null || status.isBlank()) ? null : status;
        return repository.findByFilters(normalizedSearch, normalizedStatus, pageable);
    }

    public Map<String, Object> stats() {
        Map<String, Long> statusCounts = new java.util.LinkedHashMap<>();
        for (Object[] row : repository.countByStatus()) {
            statusCounts.put((String) row[0], (Long) row[1]);
        }
        return Map.of(
                "total", repository.count(),
                "status_counts", statusCounts,
                "revenue", repository.sumRevenue()
        );
    }

    public OrderEntity requireByOrderNo(String orderNo) {
        return repository.findByOrderNoIgnoreCase(orderNo)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    @Transactional
    public OrderEntity simulateOrder(Long productId, int quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("商品不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("下单数量必须大于 0");
        }

        Map<String, Object> payload = productClient.getProduct(productId);
        String status = String.valueOf(payload.get("status"));
        int available = ((Number) payload.get("available")).intValue();
        BigDecimal unitPrice = new BigDecimal(String.valueOf(payload.get("unit_price")));
        String productName = String.valueOf(payload.get("name"));

        if (!"在售".equals(status)) {
            throw new IllegalStateException("商品当前不可下单");
        }
        if (available < quantity) {
            throw new IllegalStateException("可用库存不足");
        }

        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));
        String createdAt = FORMATTER.format(LocalDateTime.now());

        OrderEntity entity = new OrderEntity(
                "PENDING",
                total,
                "配送中",
                "模拟用户",
                "13812345678",
                "南京市建邺区河西大街66号",
                "PENDING",
                "中通",
                "苏州中转中心",
                productName + "(" + quantity + ")",
                productId,
                quantity,
                createdAt
        );

        // Persist first to get auto-generated ID
        entity = repository.saveAndFlush(entity);
        Long id = entity.getId();
        entity.setOrderNo("JAVA-ORDER-" + String.format("%03d", id));
        entity.setTrackingNo("SIM" + id);
        entity = repository.save(entity);

        SagaContext context = new SagaContext()
                .put("productId", productId)
                .put("quantity", quantity)
                .put("order", entity);

        new SagaOrchestrator("simulate-order")
                .addStep(new SagaStep(
                        "decrease-inventory",
                        sagaContext -> {
                            inventoryClient.decreaseInventory(productId, new QuantityRequest(quantity));
                            return sagaContext;
                        },
                        sagaContext -> inventoryClient.increaseInventory(productId, new QuantityRequest(quantity))
                ))
                .execute(context);

        if (rabbitTemplate != null) {
            try {
                OrderCreatedEvent event = new OrderCreatedEvent(
                        entity.getId(),
                        entity.getOrderNo(),
                        null,
                        entity.getTotalAmount(),
                        List.of(new OrderCreatedEvent.OrderItem(productId, productName, quantity, unitPrice)),
                        LocalDateTime.now()
                );
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.EXCHANGE_NAME,
                        RabbitMqConfig.ORDER_CREATED_ROUTING_KEY,
                        event
                );
                log.info("OrderCreatedEvent published for orderNo={}", entity.getOrderNo());
            } catch (Exception e) {
                log.error("Failed to publish OrderCreatedEvent for orderNo={}: {}",
                        entity.getOrderNo(), e.getMessage(), e);
            }
        }

        return entity;
    }
}
