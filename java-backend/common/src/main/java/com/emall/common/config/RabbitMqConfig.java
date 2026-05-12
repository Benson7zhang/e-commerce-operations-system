package com.emall.common.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@ConditionalOnProperty(prefix = "emall.infra", name = "mq-enabled", havingValue = "true")
public class RabbitMqConfig {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqConfig.class);

    public static final String EXCHANGE_NAME = "emall.exchange";

    public static final String ORDER_CREATED_QUEUE = "order.created.queue";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";

    public static final String INVENTORY_CHANGED_QUEUE = "inventory.changed.queue";
    public static final String INVENTORY_CHANGED_ROUTING_KEY = "inventory.*";

    public static final String RETURN_COMPLETED_QUEUE = "return.completed.queue";
    public static final String RETURN_COMPLETED_ROUTING_KEY = "return.completed";

    // ---- Exchange ----

    @Bean
    public TopicExchange emallExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    // ---- Queues ----

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue inventoryChangedQueue() {
        return new Queue(INVENTORY_CHANGED_QUEUE, true);
    }

    @Bean
    public Queue returnCompletedQueue() {
        return new Queue(RETURN_COMPLETED_QUEUE, true);
    }

    // ---- Bindings ----

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange emallExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(emallExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryChangedBinding(Queue inventoryChangedQueue, TopicExchange emallExchange) {
        return BindingBuilder.bind(inventoryChangedQueue).to(emallExchange).with(INVENTORY_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding returnCompletedBinding(Queue returnCompletedQueue, TopicExchange emallExchange) {
        return BindingBuilder.bind(returnCompletedQueue).to(emallExchange).with(RETURN_COMPLETED_ROUTING_KEY);
    }

    // ---- JSON Message Converter ----

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ---- RabbitTemplate ----

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);

        // Publisher Confirm callback
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Publisher confirm received, correlationData={}", correlationData);
            } else {
                log.error("Publisher confirm NACK, correlationData={}, cause={}", correlationData, cause);
            }
        });

        // Return callback
        template.setReturnsCallback(returned -> {
            log.error("Message returned: exchange={}, routingKey={}, replyText={}, message={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyText(),
                    returned.getMessage());
        });

        return template;
    }

    // ---- Listener Container Factory with Manual ACK ----

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            SimpleRabbitListenerContainerFactoryConfigurer configurer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        return factory;
    }
}
