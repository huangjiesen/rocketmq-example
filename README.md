# Apache RocketMQ 部署/开发指南
* [官方开发者指南](https://github.com/apache/rocketmq/tree/master/docs/cn)
* [官方docker部署指南](https://github.com/apache/rocketmq-docker)


# 本示例部署笔记
[RokectMQ for docker部署](./RokectmqForDocker.md)

# spring boot 集成 RocketMQ
## 1. 添加maven依赖
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-stream-rocketmq</artifactId>
    <version>2.2.3.RELEASE</version>
</dependency>
```
## 2. 添加配置
```properties
# rocketmq nameserver地址
rocketmq.name-server: 192.168.1.71:9876;192.168.1.76:9876;192.168.1.77:9876
# 生产组名称
rocketmq.producer.group: ${spring.application.name}
```

## 3. 生产者-RocketMQTemplate示例
[RocketMQTemplateController](src/main/java/com/ofwiki/dm/rocketmq/controller/RocketMQTemplateController.java)
```java
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
                // 执行本地事务，如：userService.save(data)，这个方法是同步执行的
                // 模拟本地事务耗时5s Thread.sleep(5000);
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
```
## 消费者-`@RocketMQMessageListener`示例
[@RocketMQMessageListener示例](src/main/java/com/ofwiki/dm/rocketmq/consumer/OrderSuccessListener.java)
```java
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
```


