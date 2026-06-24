package com.ustccb.mall.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 削峰填谷与可靠性配置
 */
@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "mall.order.exchange";
    public static final String ORDER_QUEUE = "mall.order.queue";
    public static final String ORDER_ROUTING_KEY = "order.create";

    // 死信队列（DLX）配置：用于存放异常的下单失败消息，便于人工排障和记录
    public static final String DEAD_EXCHANGE = "mall.order.dead.exchange";
    public static final String DEAD_QUEUE = "mall.order.dead.queue";
    public static final String DEAD_ROUTING_KEY = "order.dead";

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderQueue() {
        Map<String, Object> args = new HashMap<>();
        // 当消息消费失败（如被 NACK）时，自动转发至死信交换机进行处理
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_ROUTING_KEY);
        return new Queue(ORDER_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE, true, false);
    }

    @Bean
    public Queue deadQueue() {
        return new Queue(DEAD_QUEUE, true, false, false);
    }

    @Bean
    public Binding deadBinding(Queue deadQueue, DirectExchange deadExchange) {
        return BindingBuilder.bind(deadQueue).to(deadExchange).with(DEAD_ROUTING_KEY);
    }
}
