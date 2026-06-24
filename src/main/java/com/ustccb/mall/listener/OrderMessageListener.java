package com.ustccb.mall.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.ustccb.mall.config.RabbitMQConfig;
import com.ustccb.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 异步秒杀下单消息监听器 (消费端可靠性架构)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMessageListener {

    private final OrderService orderService;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String msgBody = new String(message.getBody());
        log.info("[OrderMessageListener] 收到异步下单消息: {}", msgBody);

        try {
            // 1. 反序列化消息体
            Map<String, Object> payload = objectMapper.readValue(msgBody, Map.class);
            Long userId = ((Number) payload.get("userId")).longValue();
            Long goodsId = ((Number) payload.get("goodsId")).longValue();
            Integer quantity = ((Number) payload.get("quantity")).intValue();
            String idempotentKey = (String) payload.get("idempotentKey");

            // 2. 幂等性校验：基于 Redis SETNX 防范同一条 MQ 消息被重复消费（防止网络抖动导致的重试投递）
            String redisKey = "mall:mq:idempotent:" + idempotentKey;
            Boolean success = redis.opsForValue().setIfAbsent(redisKey, "1", java.time.Duration.ofHours(24));
            if (!Boolean.TRUE.equals(success)) {
                log.warn("[OrderMessageListener] 发现重复消费消息，幂等拦截并直接 ACK. Key={}", idempotentKey);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 3. 执行核心数据库落库逻辑 (扣减数据库库存 + 写入订单)
            // 该方法内部使用 TransactionTemplate，能够保证强一致性
            orderService.doCreate(userId, goodsId, quantity);

            // 4. 手动确认：消费端成功 ACK，消息将从 RabbitMQ 队列中永久删除
            channel.basicAck(deliveryTag, false);
            log.info("[OrderMessageListener] 订单消息落库处理成功，ACK完毕. Key={}", idempotentKey);

        } catch (Exception e) {
            log.error("[OrderMessageListener] 订单消息处理异常，拒绝应答并塞入死信队列 (DLX): {}", msgBody, e);
            // 5. 拒绝消费：requeue = false 使得该消息根据绑定配置流转到死信队列，防止死循环阻塞正常通道
            channel.basicReject(deliveryTag, false);
        }
    }
}
