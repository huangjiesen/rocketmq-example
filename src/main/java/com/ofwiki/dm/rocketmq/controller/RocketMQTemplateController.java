package com.ofwiki.dm.rocketmq.controller;


import com.ofwiki.dm.rocketmq.dto.Result;
import com.ofwiki.dm.rocketmq.utils.DateUtils;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.TransactionSendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.annotation.RocketMQTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionListener;
import org.apache.rocketmq.spring.core.RocketMQLocalTransactionState;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author HuangJS
 * @date 2020-11-19 11:20 上午
 */
@RestController
@RequestMapping("rocketmq_test")
public class RocketMQTemplateController {
    private final Logger logger = LoggerFactory.getLogger(RocketMQTemplateController.class);
    @Autowired
    private RocketMQTemplate rocketMQTemplate;


    //
    // 消息生产者，消息消费者，生产者组，消费者组，顺序消息，主题，标签，Broker Server，Name Server等基本概念
    // 参见：https://github.com/apache/rocketmq/blob/master/docs/cn/concept.md
    //
    //
    //

    // @ApiOperation("1.同步发送消息,可以立马获取到结果")
    @GetMapping("sync_send")
    public Result syncSend(String msg,String topic,String tags) {
        msg += DateUtils.format(LocalDateTime.now());
        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .build();

        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        SendResult sendResult = rocketMQTemplate.syncSend(destination, message);
        logger.info("同步发送消息完成,发送结果：{}", sendResult);
        return Result.success(sendResult);
    }

    // @ApiOperation("2.异步发送消息，发送结果异步返回")
    @GetMapping("async_send")
    public Result asyncSend(String msg,String topic,String tags) {
        msg += DateUtils.format(LocalDateTime.now());
        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .build();

        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        rocketMQTemplate.asyncSend(destination, message, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                logger.info("异步发送消息成功，result:" + sendResult);
            }

            @Override
            public void onException(Throwable e) {
                // 做一些数据补偿或其它处理
                logger.error("异步发送消息失败,msg:"+e.getMessage(), e);
            }
        });
        return Result.success();
    }

    // @ApiOperation("3.异步发送消息，没有返回结果，不能确保消息是否发送成功,性能最好")
    @GetMapping("send_one_way")
    public Result sendOneway(String msg,String topic,String tags) {
        msg += DateUtils.format(LocalDateTime.now());
        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .build();

        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        logger.info("异步发送消息,destination:{},message:{}", destination, message);
        rocketMQTemplate.sendOneWay(destination, message);
        return Result.success();
    }

    // @ApiOperation("4.发送顺序消息")
    @GetMapping("send_send_orderly")
    public Result syncSendOrderly(String msg,String topic,String tags,String hashKey) {
        msg += DateUtils.format(LocalDateTime.now());
        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .build();

        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        // hashKey: 根据此hash键分配消息到队列，orderId, productId ...
        //   1.mysql binlog同步依赖严格顺序执行sql
        //   2.订单产生了3条消息，分别是订单创建、付款、完成。给用户发送订单状态提醒就得严格按照这个顺序进行消费。避免状态颠倒混乱
        // 根据hashKey将同一组消息分配到相同的队列，然后消费端顺序消费队列就能保存消息的消费顺序

        // 消费端消费模式也必需是顺序模式 ConsumeMode.ORDERLY

        SendResult sendResult = rocketMQTemplate.syncSendOrderly(destination, message, hashKey);
        logger.info("发送顺序消息,发送结果：{}", sendResult);
        return Result.success(sendResult);
    }

    // @ApiOperation("5.发送延时消息")
    @GetMapping("send_delay_time")
    public Result syncSendDelayTime(String msg,String topic,String tags) {
        msg += DateUtils.format(LocalDateTime.now());

        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .build();

        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        // 场景:比如电商里，提交了一个订单就可以发送一个延时消息，1h后去检查这个订单的状态，如果还是未付款就取消订单释放库存。
        // 延时消息的使用限制:
        //    1. 现在RocketMq并不支持任意时间的延时，需要设置几个固定的延时等级，从1s到2h分别对应着等级1到18 消息消费失败会进入延时消息队列,消息发送时间与设置的延时等级和重试次数有关
        //    2. 默认18个等级对应的时长：1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h

        SendResult sendResult = rocketMQTemplate.syncSend(
                destination, message,
                // 发送超时毫秒数
                2000,
                //设置延时等级3,这个消息将在10s之后发送
                3
        );
        logger.info("发送延时消息,发送结果：{}", sendResult);
        return Result.success(sendResult);
    }

    // @ApiOperation("6.发送事务消息")
    @GetMapping("send_batch")
    public Result syncBatch(String msg,String topic,String tags) {
        msg += DateUtils.format(LocalDateTime.now());

        Message message = MessageBuilder
                // 消息内容,泛型
                .withPayload(msg)
                // key：可通过key查询消息轨迹，如消息被谁消费，定位消息丢失问题。由于是哈希索引，须保证key尽可能唯一
                .setHeader(MessageConst.PROPERTY_KEYS, UUID.randomUUID().toString())
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.TEXT_PLAIN_VALUE)
                .setHeader("transId",UUID.randomUUID().toString())
                .build();


        // topic ：主题，消息必须发送到topic
        // tags  : 标签，可以根据不同业务目的在同一主题下设置不同标签，消费者可以根据Tag实现对不同的不同消费逻辑
        //       tags从命名来看像是一个复数，但发送消息时，目的地只能指定一个topic下的一个tag，不能指定多个

        // destination: 的格式为topicName:tagName
        String destination = topic + ":" + tags;

        // 同步得到本地事务执行结果
        TransactionSendResult sendResult = rocketMQTemplate.sendMessageInTransaction("tx_producer_group",destination, message, msg);
        logger.info("2.发送事务消息,发送结果：{}", sendResult);
        return Result.success(sendResult);
    }



    @Component
    @RocketMQTransactionListener(txProducerGroup = "tx_producer_group")
    public static class SyncProducerListener implements RocketMQLocalTransactionListener {
        private final Logger logger = LoggerFactory.getLogger(SyncProducerListener.class);
        private ConcurrentHashMap<String, RocketMQLocalTransactionState> localTrans = new ConcurrentHashMap<>();


        @Override
        public RocketMQLocalTransactionState executeLocalTransaction(Message message, Object data) {
            String key = String.valueOf(message.getHeaders().get("transId"));
            localTrans.put(key, RocketMQLocalTransactionState.UNKNOWN);
            try {
                // 做一下本事务，如：userService.save(data)，这个方法是同步执行的
                Thread.sleep(5000);

                if (new Random().nextInt() % 2 == 0) {
                    throw new Exception("本地事务异常");
                }

                logger.info("1.【本地业务执行完毕】 msg:{}, Object:{}", message, data);
                localTrans.put(key, RocketMQLocalTransactionState.COMMIT);
            } catch (Exception e) {
                e.printStackTrace();

                if (new Random().nextInt() % 2 == 0) {
                    throw new RuntimeException("没有办法确认本地事务状态,message key:" + key);
                }

                logger.error("1.【执行本地业务异常】 exception message:{}", e.getMessage());
                localTrans.put(key, RocketMQLocalTransactionState.ROLLBACK);
            }
            return localTrans.get(message.getHeaders().getId());
        }

        @Override
        public RocketMQLocalTransactionState checkLocalTransaction(Message message) {
            String key = String.valueOf(message.getHeaders().get("transId"));
            RocketMQLocalTransactionState state = localTrans.get(key);
            logger.info("3.【执行检查任务】:message key{},trans stats:{},",key,state);
            if (state != null) {
                return state;
            }
            logger.info("4.【执行检查任务】状态为空，默认提交事务，message key：{}",key);
            return RocketMQLocalTransactionState.COMMIT;
        }
    }
}
