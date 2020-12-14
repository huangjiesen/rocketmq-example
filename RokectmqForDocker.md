# 部署方案
* 版本: 4.7.1
* 集群模式: RocketMQ-on-DLedger Group 三节点集群broker自动容灾切换
  * RocketMQ-on-DLedger Group 是指一组相同名称的 Broker，至少需要 3 个节点，通过 Raft 自动选举出一个 Leader，其余节点 作为 Follower，并在 Leader 和 Follower 之间复制数据以保证高可用。
  * RocketMQ-on-DLedger Group 能自动容灾切换，并保证数据一致。
  * RocketMQ-on-DLedger Group 是可以水平扩展的，也即可以部署任意多个
  * RocketMQ-on-DLedger Group 同时对外提供服务。

其他集群部署模式介绍

1. `单Master模式`: 重启或者宕机时，会导致整个服务不可用
2. `多Master模式`: 单台机器宕机期间, 对应用的非顺序消息发送无影响 这台机器上未被消费的消息在机器恢复之前不可订阅
3. `多Master多Slave模式-异步复制`: Master宕机后，消费者仍然可以从Slave消费。Master宕机，磁盘损坏情况下会丢失少量消息。
4. `多Master多Slave模式-同步双写`: 性能比异步复制模式略低，发送单个消息的RT会略高，主节点宕机后，备机不能自动切换为主机

参见：https://github.com/apache/rocketmq/tree/master/docs/cn



# 部署范围
* 192.168.1.71 一个nameserver实例,两个broker实例,一个console
* 192.168.1.76 一个nameserver实例,两个broker实例
* 192.168.1.77 一个nameserver实例,两个broker实例


# 部署步骤
## 1. 创建工作目录
```
mkdir -p /data/docker-services/rocketmq
cd /data/docker-services/rocketmq
```

## 2. 构建rocketmq镜像
在`${work_home}`目录下克隆rocketmq docker镜像构建脚本
```
git clone https://github.com/apache/rocketmq-docker.git
cd rocketmq-docker/image-build
sh build-image.sh 4.7.1 alpine 
cd /data/docker-services/rocketmq
```
参见：https://github.com/apache/rocketmq-docker

## 3. nameserver 服务部署
### 3.1 创建nameserver工作目录
```
mkdir -p /data/docker-services/rocketmq/namesrv/logs
```
* 工作目录:/data/docker-services/rocketmq/namesrv
* 日志目录:/data/docker-services/rocketmq/namesrv/logs

### 3.2 nameserver服务编排
编写docker-compose.yml文件：
```
vi /data/docker-services/rocketmq/namesrv/docker-compose.yml
```
内容如下:
```
version: "2"
networks:
  default:
    external:
      name: bridge_network
services:
  #Service for nameserver
  rocketmq-namesrv:
    image: apacherocketmq/rocketmq:4.7.1-alpine
    container_name: rocketmq-namesrv
    ports:
      - 9876:9876
    volumes:
      - ./logs:/home/rocketmq/logs
    command: sh mqnamesrv
    restart: always
```
### 3.3 启动nameserver服务
```
docker-compsoe -f /data/docker-services/rocketmq/namesrv/docker-compose.yml up
```

## 4. broker服务部署

### 4.1 创建broker工作目录
```
mkdir -p /data/docker-services/rocketmq/broker/data/broker0
mkdir -p /data/docker-services/rocketmq/broker/logs/broker0
chmod a+rw /data/docker-services/rocketmq/broker/data/broker0
chmod a+rw /data/docker-services/rocketmq/broker/logs/broker0


mkdir -p /data/docker-services/rocketmq/broker/data/broker1
mkdir -p /data/docker-services/rocketmq/broker/logs/broker1
chmod a+rw /data/docker-services/rocketmq/broker/data/broker1
chmod a+rw /data/docker-services/rocketmq/broker/logs/broker1

mkdir -p /data/docker-services/rocketmq/broker/conf/dledger
```

### 4.2 broker服务编排
编写docker-compose.yml文件：
```
vi /data/docker-services/rocketmq/broker/docker-compose.yml
```
内容如下:
```
version: "2"
networks:
  default:
    external:
      name: bridge_network
services:
  #Service for broker
  rocketmq-broker0:
    image: apacherocketmq/rocketmq:4.7.1-alpine
    container_name: rocketmq-broker0
    ports:
      - 10909:10909
      - 10911:10911
      - 10912:10912
      - 20911:20911
    volumes:
      - ./logs/broker0:/home/rocketmq/logs
      - ./data/broker0:/home/rocketmq/store
      - ./conf/dledger:/opt/rocketmq-4.7.1-alpine/conf/dledger
    command: sh mqbroker -c /opt/rocketmq-4.7.1-alpine/conf/dledger/broker-n0.conf
    restart: always
  rocketmq-broker1:
    image: apacherocketmq/rocketmq:4.7.1-alpine
    container_name: rocketmq-broker1
    ports:
      - 10919:10919
      - 10921:10921
      - 10922:10922
      - 20921:20921
    volumes:
      - ./logs/broker1:/home/rocketmq/logs
      - ./data/broker1:/home/rocketmq/store
      - ./conf/dledger:/opt/rocketmq-4.7.1-alpine/conf/dledger
    command: sh mqbroker -c /opt/rocketmq-4.7.1-alpine/conf/dledger/broker-n1.conf
    restart: always
```
### 4.3 broker配置文件
```
vi /data/docker-services/rocketmq/broker/conf/dledger/broker-n0.conf
```
broker节点0的配置文件,内容如下:
```
# 接受客户端连接的监听端口,默认10911，producer和consumer连接该端口
listenPort=10911
#表示Master监听Slave请求的端口,默认为 listenPort+1
# haListenPort=10912
# vip 通道端口,默认为 listenPort+2
# fastListenPort=10909

# nameServer 地址
namesrvAddr = 192.168.1.71:9876;192.168.1.76:9876;192.168.1.77:9876
# 当前 broker 监听的 IP
brokerIP1 = 192.168.1.71
# 存在主从 broker 时，如果在 broker 主节点上配置了 brokerIP2 属性，broker 从节点会连接主节点配置的 brokerIP2 进行同步
brokerIP2 = 192.168.1.71
# broker 的名称
brokerName = RaftNode00
# 本 broker 所属的 Cluser 名称
brokerClusterName = RaftCluster
# broker id, 0 表示 master, 其他的正整数表示 slave, DLedger模式可不指定
# brokerId = 0
# 在每天的什么时间删除已经超过文件保留时间的 commit log
deleteWhen = 04
# 以小时计算的文件保留时间
fileReservedTime = 72
# Broker 角色
#   SYNC_MASTER: 同步主机
#   ASYNC_MASTER: 异步主机
#   SLAVE: 从机
# 如果对消息的可靠性要求比较严格，可以采用 SYNC_MASTER加SLAVE的部署方式。如果对消息可靠性要求不高，可以采用ASYNC_MASTER加SLAVE的部署方式。
brokerRole = ASYNC_MASTER
# 数据刷盘模式
#   SYNC_FLUSH:同步刷新, 模式下的 broker 保证在收到确认生产者之前将消息刷盘。
#   ASYNC_FLUSH:异步处理, 模式下的 broker 则利用刷盘一组消息的模式，可以取得更好的性能。
# SYNC_FLUSH（同步刷新）相比于ASYNC_FLUSH（异步处理）会损失很多性能，但是也更可靠，所以需要根据实际的业务场景做好权衡。
flushDiskType = ASYNC_FLUSH


#########  DLedger 容灾切换相关配置 start  #########
# 是否启动 DLedger 容灾切换能力,启动 DLedger 至少需要 3 个节点,才能容忍其中 1 个宕机
enableDLegerCommitLog=true
# DLedger Raft Group的名字，建议和 brokerName 保持一致
dLegerGroup=RaftNode00
# DLedger Group 内各节点的端口信息，同一个 Group 内的各个节点配置必须要保证一致
dLegerPeers=n0-192.168.1.71:20911;n1-192.168.1.76:20911;n2-192.168.1.77:20911
# 节点 id, 必须属于 dLegerPeers 中的一个；同 Group 内各个节点要唯一
dLegerSelfId=n0
# 发送线程个数，建议配置成 Cpu 核数
sendMessageThreadPoolNums=2
######### DLedger 容灾切换相关配置 end  #########
```
```
vi /data/docker-services/rocketmq/broker/conf/dledger/broker-n1.conf
```
broker节点1的配置文件,内容如下:
```
# 接受客户端连接的监听端口,默认10911，producer和consumer连接该端口
listenPort=10921
#表示Master监听Slave请求的端口,默认为 listenPort+1
# haListenPort=10912
# vip 通道端口,默认为 listenPort+2
# fastListenPort=10909

# nameServer 地址
namesrvAddr = 192.168.1.71:9876;192.168.1.76:9876;192.168.1.77:9876
# 当前 broker 监听的 IP
brokerIP1 = 192.168.1.71
# 存在主从 broker 时，如果在 broker 主节点上配置了 brokerIP2 属性，broker 从节点会连接主节点配置的 brokerIP2 进行同步
brokerIP2 = 192.168.1.71
# broker 的名称
brokerName = RaftNode01
# 本 broker 所属的 Cluser 名称
brokerClusterName = RaftCluster

# broker id, 0 表示 master, 其他的正整数表示 slave , DLedger模式可不指定
# brokerId = 0

# 在每天的什么时间删除已经超过文件保留时间的 commit log
deleteWhen = 04
# 以小时计算的文件保留时间
fileReservedTime = 72
# Broker 角色
#   SYNC_MASTER: 同步主机
#   ASYNC_MASTER: 异步主机
#   SLAVE: 从机
# 如果对消息的可靠性要求比较严格，可以采用 SYNC_MASTER加SLAVE的部署方式。如果对消息可靠性要求不高，可以采用ASYNC_MASTER加SLAVE的部署方式。
brokerRole = ASYNC_MASTER
# 数据刷盘模式
#   SYNC_FLUSH:同步刷新, 模式下的 broker 保证在收到确认生产者之前将消息刷盘。
#   ASYNC_FLUSH:异步处理, 模式下的 broker 则利用刷盘一组消息的模式，可以取得更好的性能。
# SYNC_FLUSH（同步刷新）相比于ASYNC_FLUSH（异步处理）会损失很多性能，但是也更可靠，所以需要根据实际的业务场景做好权衡。
flushDiskType = ASYNC_FLUSH


#########  DLedger 容灾切换相关配置 start  #########
# 是否启动 DLedger 容灾切换能力,启动 DLedger 至少需要 3 个节点,才能容忍其中 1 个宕机
enableDLegerCommitLog=true
# DLedger Raft Group的名字，建议和 brokerName 保持一致
dLegerGroup=RaftNode01
# DLedger Group 内各节点的端口信息，同一个 Group 内的各个节点配置必须要保证一致
dLegerPeers=n0-192.168.1.71:20921;n1-192.168.1.76:20921;n2-192.168.1.77:20921
# 节点 id, 必须属于 dLegerPeers 中的一个；同 Group 内各个节点要唯一
dLegerSelfId=n0
# 发送线程个数，建议配置成 Cpu 核数
sendMessageThreadPoolNums=2
######### DLedger 容灾切换相关配置 end  #########
```

### 4.4 启动broker服务
启动broker服务前
* 确保至少一个nameserver启动
* 修改/broker-n0.conf,/broker-n1.conf配置文件的brokerIP1,brokerIP2为具体的IP,改为71、75或76
```
docker-compsoe -f /data/docker-services/rocketmq/broker/docker-compose.yml up
```

## 5. console服务部署
### 5.1 创建console工作目录
```
mkdir /data/docker-services/rocketmq-console
```
### 5.2 服务编排
编写docker-compose.yml文件：
```
vi /data/docker-services/rocketmq-console/docker-compose.yml
```
内容如下:
```
version: "2"
networks:
  default:
    external:
      name: bridge_network
services:
  rocketmq-console:
    image: apacherocketmq/rocketmq-console:2.0.0
    container_name: rocketmq-console
    environment:
      - "JAVA_OPTS=-Drocketmq.namesrv.addr=192.168.1.71:9876;192.168.1.76:9876;192.168.1.77:9876 -Dcom.rocketmq.sendMessageWithVIPChannel=false"
    ports:
      - 19876:8080
    restart: always
```
### 5.3 启动服务
```
docker-compose -f /data/docker-services/rocketmq-console/docker-compose.yml up 
```
访问`http://192.168.1.71:19876`,进入RocketMQ控制台


## 6. 自动容灾切换测试
进入RocketMQ控制台，可查看Broker集群状态。将master节点示例停掉,大概10s自动完成master切换。
