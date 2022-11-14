# Agent 迁移程序
### 1、使用前确认
在迁移前需确认新 agent 已开启 **web 模式**。
```shell
# 在新 agent 部署机器上执行命令
$ curl localhost:8888/export/health
# 若出现类似这样的结果，则代表 web 已启动
# 若 status 为 UP ，代表新 agent 已经在解析日志了。迁移所做的操作可能会影响其运行状态。
{"status":"DOWN","checks":[...]}
```
### 2、迁移程序目录结构
迁移程序使用的是一个 tar 包，名称为 **agent-migrate-linux-1.0-SNAPSHOT.tar**。

执行 tar -xvf agent-migrate-linux-1.0-SNAPSHOT.tar 解压，新建出 agent-migrate 的文件夹。

agent-migrate 文件夹内由以下几部分组成：
- agent-migrate-1.0-SNAPSHOT.jar
- conf
- log4j.properties
- result
- start.sh

介绍：
#### agent-migrate-1.0-SNAPSHOT.jar
作用：程序 jar 包，代码修改也可以直接更新此 jar 包
#### conf（文件夹）
作用：存放需要的配置信息<br>
包含：config.yml<br>
配置文件示例：
```yaml
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
#### log4j.properties
作用：日志配置文件<br>
默认为 **console** 模式，即打印到控制台。<br>
如果将第一行修改为：
``` sh
log4j.rootLogger=info,file
```
则会将日志打印至 **update.log** 文件中
#### result（文件夹）
作用：存放迁移过程中持久化的一些信息<br>
包含：config.json、data.json
#### start.sh
作用：启动脚本<br>
参数：<br>
-h | --help : 查看帮助信息<br>
-d n | --debug n : 开启 debug 模式（suspend=no）
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
### 4、执行迁移
运行 ./start.sh 