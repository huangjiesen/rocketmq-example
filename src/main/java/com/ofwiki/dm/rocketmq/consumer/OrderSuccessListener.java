package com.ofwiki.dm.rocketmq.consumer;

import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author HuangJS
 * @date 2020-11-23 10:50 上午
 */
@Component
@RocketMQMessageListener(
        // 同消费组的，主题和标签必须一致，否则新启动的实例会覆盖原有实例，可能会出消息丢失的情况
        consumerGroup = "orderSuccessGroup",
        // 消费主题
        topic = "shop_order_topic",
        // 消费标签，可以不指定或指定一到多个，多个用 || 连接
        selectorExpression = "order.status.success || order.status.complete",
        // 消费模式:
        //    1.顺序消费:一个线程消费一个队列，从而保证队列的消费有顺序的
        //    2.并行消费:多个线程消费相同的队列，没有顺序保证，但消费消费快 - 默认
        consumeMode = ConsumeMode.ORDERLY
        // 最大线程数
        // ,consumeThreadMax = 64
        // 消息模式:
        //    1.集群消费: 相同Consumer Group的每个Consumer实例平均分摊消息。
        //    2.广播消费: 相同Consumer Group的每个Consumer实例都接收全量的消息。
        // ,messageModel = MessageModel.CLUSTERING
)
public class OrderSuccessListener implements RocketMQListener<String> {
    private Logger logger = LoggerFactory.getLogger(OrderSuccessListener.class);


    @Override
    public void onMessage(String message) {
        logger.info("订单已success：" + message);
    }
}
