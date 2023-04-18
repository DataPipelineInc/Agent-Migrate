# Agent 迁移程序
## 一、迁移程序介绍及使用前确认
### 1、迁移程序目录结构
迁移程序使用的是一个 tar 包，名称为 **agent-migrate-linux-1.0-SNAPSHOT.tar**。

执行
``` shell
tar -xvf agent-migrate-linux-1.0-SNAPSHOT.tar
```
解压，新建出 agent-migrate 文件夹。

agent-migrate 文件夹由以下几部分组成：
- agent-migrate-1.0-SNAPSHOT.jar
- conf
- result
- start.sh

介绍：
#### agent-migrate-1.0-SNAPSHOT.jar
作用：程序 jar 包，代码修改也可以直接更新此 jar 包
#### conf（文件夹）
作用：存放需要的配置信息<br>
包含：config.yml<br>
配置文件示例：
``` yaml
# 旧 agent 的主机 ip、web 端口、部署根路径、token 信息
old:
  host: 82.157.57.48
  web_port: 8303
  path: /home/zc-agent/
  token: dVUL0Yinqodc4FjYC7Lw1LUANgN7XHtb
# 新 agent 的 srcId、主机 ip、web 端口、是否使用旧 agent 的序列化方式
new:
  src_id: 6
  host: 82.157.57.48
  web_port: 8888
  # string 代表使用旧 agent 的序列化方式，默认值为 avro
  migrate_topic_format: string
# DP的主机 ip、web 端口、kafka 地址、token 信息
dp:
  host: 81.70.62.93
  web_port: 5000
  kafka_bootstrap_servers: 192.168.0.31:9092
  token: ECswrmIZn05LQcFa/2ww8bnnICYKMZtK6MYzrqON6NmuGpyHs0vQNAkrVYH1QX+1
```

**提示信息（重要！！！）：**
```
配置文件中的 migrate_topic_format: string 属性，在 DP 以下版本中生效。
如果不满足版本要求还将属性值设置为 string 的话，可能会有 DP 任务按照错误的反序列化方式消费的风险。
3.5版本：0.23.48及以上
3.6版本：0.24.10及以上
```

#### result（文件夹）
作用：存放迁移过程中持久化的一些信息<br>
包含：config.json、data.json
#### start.sh
作用：启动脚本<br>

### 2、使用前确认
#### 新 agent 的迁移前确认：

1、执行命令查看新 agent 的健康状态（ host 和 port 即 config.yml 文件中的 new.host 和 new.web_port 属性）

``` sh
# 在新 agent 部署机器上执行命令
$ curl <host>:<port>/export/health
```

2、若显示 Connection Refused，代表新 agent 的 web 未开启，需在新 agent 部署路径执行 ./start.sh -w

3、若返回类似这样的结果，则代表 web 已启动

``` sh
{"status":"DOWN","checks":[...]}
```

4、若 status 的值为 "UP" ，代表新 agent 已经在解析日志了。迁移所做的操作可能会影响其运行状态，可执行下面的命令停止

``` sh
$ curl -XPOST <host>:<port>/export/stop
```
#### DP 的迁移前确认：

根据 config.yml 文件中的 new.src_id 值去查找 DP 中对应的节点信息，确认该节点包含了旧 agent 的配置信息
### 3、如何填写配置中的 Token 信息
#### 旧 agent：
1. 登录旧 agent 的源端
2. 打开开发者模式
3. 点击同步对象
4. 点击 Network（网络）中的 getusermap 请求
5. 查看 Request Headers（请求标头） 中 Cookie 的 token 值
#### DP：
1. 登录 DP
2. 打开开发者模式
3. 点击 Application（应用） 中的 Local Storage（本地存储空间）
4. 查看 DP 所在服务器的 token 值

## 二、迁移步骤
### 1、修改配置文件
在解压出的 agent-migrate 文件夹下，进入 conf 文件夹，修改 config.yml
### 2、执行迁移
在 agent-migrate 文件夹下执行
``` sh
# 默认方式，按照 配置迁移 -> 进度迁移 -> 数据迁移 顺序依次执行
./start.sh
# 从 进度迁移 开始执行
./start.sh -p
# 从 数据迁移 开始执行
./start.sh -l
```

## 三、影响分析及异常回滚
**配置迁移**

作用：迁移同步对象信息和数据库信息

回滚操作：迁移程序自动将新 agent 实例的 map.yml 和 oracle.yml 恢复至默认值<br>

异常处理：不影响旧 agent 和 DP 的运行，无需手动处理

备注：持久化信息存储在 result 文件夹中的 config.json 文件中

old_conf ：旧 agent 实例处获取到的数据库配置信息<br>

node_config ：新 agent 实例存储的数据库配置信息<br>

**进度迁移**

作用：结合旧 agent 的日志解析进度（ cfg.loginfo ）及未提交的事务列表（ translist ）设置解析进度

影响：会停止旧 agent 实例

异常处理：如出现问题，回滚操作会重启旧 agent 实例，如果未正确重启，可以在旧 agent 的 web 界面手动重启。

**数据迁移**

作用：完成节点配置信息的更新及链路读取模式的修改

影响：

如果在 conf/config.yml 中显式设置了 migrate_topic_format: string ，则序列化方式为 **string** ，否则为 **avro**。<br>

此时旧 agent 实例已经被停止<br>

如果序列化方式为 **avro** ， agent topic 中的数据会等待全部被消费掉，然后全部清空。<br>

如果序列化方式为 **string** ，则不会清空 agent topic 数据。<br>

之后不论何种序列化方式，都会执行暂停运行中任务，修改链路对应的读取模式，启动新 agent 实例，重启任务的操作。<br>

异常处理：如出现问题，回滚操作会重启旧 agent 实例。可重新执行 ./start.sh<br>

如果序列化方式为 **avro** 且 agent topic 数据已经被清空，也可以手动修改对应链路的读取模式，重启相应任务。

备注：持久化信息存储在 result 文件夹中的 data.json 文件中

topicNames ：运行中任务所对应的 agent topic 的名称列表<br>

nineOffsets ：旧 agent 实例向 agent topic 写入数据的 offset 列表<br>

endOffsets ：topic 对应 endOffset 的 map<br>

topicConfigs ：topic 对应其配置信息的 map<br>

topicOffsets ：topic 对应 offset 的列表
