package com.emall.inventory.service;

import com.emall.common.config.RabbitMqConfig;
import com.emall.common.mq.event.OrderCreatedEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "emall.infra.mq-enabled", havingValue = "true")
public class InventoryEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventListener.class);

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(@Payload OrderCreatedEvent event,
                               Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            log.info("Received OrderCreatedEvent: orderNo={}, productId={}, quantity={}",
                    event.orderNo(),
                    event.items() != null && !event.items().isEmpty()
                            ? event.items().get(0).productId() : "N/A",
                    event.items() != null && !event.items().isEmpty()
                            ? event.items().get(0).quantity() : "N/A");

            // Placeholder for inventory pre-occupy logic
            log.info("Order created event processed, ready for inventory pre-occupy logic");

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Error processing OrderCreatedEvent: {}", e.getMessage(), e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
