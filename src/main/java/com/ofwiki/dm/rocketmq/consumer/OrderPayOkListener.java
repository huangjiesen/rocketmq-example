package com.ofwiki.dm.rocketmq.consumer;

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
        consumerGroup = "ORDER_PAY_OK_GROUP",
        topic = "shop_order_topic",
        selectorExpression = "order.status.pay_ok"
)
public class OrderPayOkListener implements RocketMQListener<String> {
    private Logger logger = LoggerFactory.getLogger(OrderPayOkListener.class);

    @Override
    public void onMessage(String message) {
        logger.info("订单已pay_ok：" + message);
    }
}
