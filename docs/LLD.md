# Unified Observability Kit (UOK) - 低层设计文档 (LLD)

> 版本：V1.1 正式版 | 适用场景：IoT 平台全链路统一观测 | 兼容环境：OpenJDK 17/21、Spring Cloud、AWS Lambda、Kafka/MSK

---

## 1. 项目整体模块划分与目录结构

```
uok-project/
├── uok-common/                # 公共核心模块
├── uok-agent/                 # Java Agent 核心模块
├── uok-lambda-starter/        # Lambda Starter 模块
├── samples/                   # 样例工程集
│   ├── sample-springcloud-service/
│   │   ├── sample-gateway/
│   │   ├── sample-service-a/
│   │   └── sample-service-b/
│   ├── sample-kafka-app/
│   │   ├── sample-kafka-producer/
│   │   └── sample-kafka-consumer/
│   └── sample-lambda-function/
├── test-suite/                # 测试套件
│   ├── unit-test/
│   ├── integration-test/
│   └── performance-test/
└── deployment/                # 部署配置
    └── filebeat-config/
```

**构建工具**：Maven 3.8+
**编译版本**：源/目标版本均为 Java 17，同步验证 Java 21 兼容性
**核心依赖版本**：

- OpenTelemetry: 1.32.0
- Spring Boot: 3.2.x（兼容 2.7.x）
- Log4j2: 2.20.x
- Testcontainers: 1.19.x
- JMH: 1.37

## 2. uok-common 公共核心模块详细设计

### 2.1 核心包结构

```
com.bosch.iot.uok.common
├── config/          # 配置解析
│   ├── UokConfig.java
│   ├── ConfigLoader.java
│   ├── GrayConfig.java          # 灰度配置
│   └── SamplerConfig.java       # 采样配置
├── context/         # Trace上下文模型
│   ├── TraceContext.java
│   ├── ContextHolder.java
│   └── ReactiveContextHolder.java  # 响应式上下文容器
├── constant/        # 常量定义
│   ├── LogConstants.java         # 日志字段常量
│   └── MetricConstants.java      # 指标名称常量
├── sampler/         # 采样策略
│   ├── Sampler.java
│   ├── HeadSampler.java          # 头部采样
│   ├── ErrorAlwaysSampler.java   # 错误全采
│   ├── TailSampler.java          # 尾部采样
│   └── DeviceRatioSampler.java   # 按设备比例采样
├── gray/            # 灰度控制
│   └── GrayController.java
├── desensitize/     # 数据脱敏
│   └── DataMasker.java
├── metrics/         # 指标采集
│   └── MetricCollector.java
├── degrade/         # 降级管控
│   └── DegradeManager.java
└── utils/           # 工具类
    └── TraceIdGenerator.java
```

### 2.2 核心配置项

| 配置 Key | 类型 | 默认值 | 说明 |
|---------|------|--------|------|
| `uok.service.name` | String | `unknown-service` | 服务名称 |
| `uok.biz.domain` | String | `default` | 业务域，用于权限隔离 |
| `uok.team.name` | String | `default` | 所属团队，用于权限隔离 |
| `uok.env` | String | 自动识别 | 环境标识，自动识别 dev/test/prod |
| `uok.agent.enabled` | Boolean | `true` | 总开关，false 则完全关闭埋点 |
| `uok.log.enable` | Boolean | `true` | 是否开启日志注入 |
| `uok.trace.enable` | Boolean | `true` | 是否开启链路追踪 |
| `uok.metrics.enable` | Boolean | `true` | 是否开启指标采集 |
| `uok.sampler.head-rate` | Double | `1.0` | 头部采样率 |
| `uok.sampler.error-always` | Boolean | `true` | 错误日志是否强制全采 |
| `uok.sampler.device-ratio` | Double | `1.0` | 按设备比例采样率（IoT 场景） |
| `uok.degrade.cpu-threshold` | Integer | `80` | CPU 阈值（%），超阈值自动降级 |
| `uok.degrade.latency-increase` | Integer | `10` | 时延增幅阈值（%），超阈值自动降级 |
| `uok.gray.service-list` | List | 空 | 灰度开启的服务列表 |
| `uok.gray.instance-ratio` | Double | `1.0` | 实例灰度比例 |

## 3. uok-agent 模块详细设计

### 3.1 核心能力封装

基于 OpenTelemetry Java Agent 进行二次封装，内置所有常用插件（Spring Web、Spring Cloud、Kafka、JDBC、Redis、DynamoDB、线程池、WebFlux），业务侧无需单独引入任何插件。

### 3.2 字节码增强覆盖范围

| 增强点 | 实现方式 | 核心逻辑 |
|--------|---------|---------|
| HTTP 入口 | Servlet 字节码增强 | 提取请求头 Trace 上下文，无则生成根上下文，注入 MDC |
| HTTP 出口 | RestTemplate/Feign/WebClient 字节码增强 | 将当前 Trace 上下文写入请求头，生成出口 Span |
| 响应式入口 | WebFlux 字节码增强 | 适配 Reactor 上下文，响应式请求链路 TraceID 不丢失 |
| Kafka 生产 | Kafka Producer 字节码增强 | 将 Trace 上下文写入消息 Header，生成发送 Span |
| Kafka 消费 | Kafka Consumer 字节码增强 | 从消息 Header 提取上下文，生成消费 Span，注入 MDC |
| 异步线程 | 线程池/CompletableFuture/@Async 字节码增强 | 上下文跨线程传递，异步日志携带 TraceID |
| 定时任务 | @Scheduled 字节码增强 | 定时任务自动生成独立 Trace，执行日志携带 TraceID |
| MySQL 操作 | JDBC 驱动字节码增强 | 生成数据库操作 Span，记录 SQL 与耗时 |
| DynamoDB 操作 | AWS SDK 字节码增强 | 拦截 DynamoDB 读写请求，生成 Span，记录操作类型与耗时 |
| Redis 操作 | Jedis/Lettuce 字节码增强 | 拦截 Redis 命令，生成 Span，记录命令与耗时 |
| 异常捕获 | 全局异常字节码增强 | 异常堆栈自动关联 TraceID，触发错误全采策略 |

### 3.3 日志注入与多维度字段逻辑

- 自动注入基础字段：`traceId`、`spanId`、`parentSpanId`、`serviceName`、`env`、`bizDomain`、`teamName`
- 业务字段自动扩展机制：
  - 可通过配置指定 HTTP 请求头、Kafka 消息 Header 中的业务字段（如 `device-id`、`user-id`），自动提取并注入 MDC；
  - 支持自定义扩展接口，业务可按需注册字段提取器，零侵入注入业务维度。

### 3.4 指标自动采集逻辑

- 自动统计每个接口/方法的：请求量、成功量、失败量、平均耗时、P50/P95/P99 耗时、错误率；
- 标准 Prometheus 格式暴露，端口可配置；
- 指标自动携带服务名、环境、接口名标签，支持多维度聚合。

## 4. uok-lambda-starter 模块详细设计

### 4.1 设计原则

纯 SDK 封装，不依赖 AWS 官方 OTel Layer，无任何 AWS 专属运行时依赖，仅依赖 AWS Lambda Java Core SDK 与 OpenTelemetry 开源 SDK，可在任意兼容 Java 的函数环境运行。

### 4.2 核心包结构

```
com.bosch.iot.uok.lambda
├── LambdaTracingInitializer.java    # 自动初始化入口
├── adapter/
│   ├── KinesisEventAdapter.java     # Kinesis事件Trace提取
│   └── KafkaEventAdapter.java       # MSK事件Trace提取
└── context/
    └── LambdaContextHolder.java
```

### 4.3 核心逻辑

1. 函数启动时自动初始化 OTel SDK，加载配置；
2. 通过事件适配器，从 Kinesis/MSK 事件记录中提取 Trace 上下文；
3. 若事件中无 TraceID，则自动生成新的根 Trace；
4. 将 Trace 上下文注入日志 MDC，函数内所有日志自动携带链路字段；
5. 函数内调用下游 HTTP/Kafka 时，自动续传链路上下文。

## 5. 样例工程详细设计

### 5.1 sample-springcloud-service

- **组成**：Spring Cloud Gateway + Service A + Service B
- **验证场景**：Gateway → Service A → Service B 三级 HTTP 调用，包含异步线程、数据库调用、Redis 调用
- **验证点**：
  1. 全链路 TraceID 一致；
  2. 每个服务 SpanID 不同，ParentSpanID 正确指向上游；
  3. 所有服务日志均携带完整链路字段与权限字段；
  4. 服务内异步线程、定时任务日志 TraceID 不丢失；
  5. MySQL、Redis 调用生成独立 Span。

### 5.2 sample-kafka-app

- **组成**：Kafka Producer 应用 + Kafka Consumer 应用
- **验证场景**：生产者发送消息 → 消费者接收并异步处理
- **验证点**：
  1. 生产端与消费端 TraceID 一致；
  2. 消费端 Span 的 ParentSpanID 指向生产端发送 Span；
  3. 消息 Body 未被修改，仅 Header 携带上下文；
  4. 消费者异步处理链路不中断。

### 5.3 sample-lambda-function

- **组成**：基于 Kinesis 触发的 Java Lambda 函数
- **验证场景**：Kinesis 流推送事件 → Lambda 消费处理 → 调用下游 HTTP 接口
- **验证点**：
  1. 可从 Kinesis 记录中提取 TraceID；
  2. Lambda 函数日志携带正确的 TraceID、SpanID；
  3. 函数内调用下游接口时链路自动续传；
  4. 无 AWS OTel Layer 依赖，纯 SDK 方式运行。

## 6. 测试体系详细设计

### 6.1 单元测试

- **覆盖范围**：uok-common 所有类、配置解析、上下文模型、采样器、脱敏工具、灰度控制器、降级管理器
- **技术栈**：JUnit 5 + Mockito
- **卡点要求**：行覆盖率 ≥ 90%，分支覆盖率 ≥ 85%
- **执行时机**：每次代码提交自动执行

### 6.2 自动化集成测试

- **技术方案**：JUnit 5 + Testcontainers
- **测试用例清单**：
  1. Spring Cloud 同步跨服务调用链路正确性测试
  2. Spring WebFlux 响应式链路透传测试
  3. Kafka 生产消费链路透传测试
  4. 线程池/@Async 异步线程 TraceID 传递测试
  5. 定时任务 Trace 生成测试
  6. MySQL/DynamoDB/Redis 存储操作埋点测试
  7. 日志字段注入正确性测试（含业务字段与权限字段）
  8. Lambda 事件提取与链路生成测试
  9. 多级采样策略生效测试（头部采样/错误全采/按设备采样）
  10. 灰度接入规则生效测试
  11. 自动降级触发与恢复测试
  12. Prometheus 指标输出正确性测试
- **验证方法**：自动解析日志文件，校验 TraceID、SpanID、ParentSpanID 的存在性与层级关系，自动断言链路完整性。

### 6.3 性能基准测试

- **技术方案**：JMH (Java Microbenchmark Harness)
- **测试维度**：
  1. QPS 对比：无 Agent vs 接入 Agent 的接口吞吐量
  2. 平均响应时延对比
  3. CPU 使用率对比
  4. 内存占用对比
  5. 高并发下稳定性测试
- **输出产物**：自动化生成性能对比报告，量化损耗百分比，超出阈值自动告警。

## 7. 链路验证方案设计

### 7.1 日志输出验证

所有样例工程均配置 Log4j2 输出结构化 JSON 日志文件，验证脚本自动扫描日志文件：

1. 校验每条日志均包含 `traceId`、`spanId` 字段；
2. 同一次请求的所有日志 `traceId` 完全一致；
3. 调用链路上游的 `spanId` 等于下游的 `parentSpanId`；
4. 根节点日志的 `parentSpanId` 为空或特定标识；
5. 错误场景日志 100% 保留，正常场景符合采样比例。

### 7.2 自动化校验工具

提供 `TraceValidator` 工具类，集成在测试套件中，可自动解析日志文件，输出链路拓扑与校验结果，支持断言集成到测试用例中。

## 8. Filebeat 对接规范

### 8.1 日志格式约定

业务日志统一输出为 JSON 格式，包含标准字段：

```json
{
  "timestamp": "2026-06-29T10:00:00.000Z",
  "level": "INFO",
  "traceId": "xxx",
  "spanId": "yyy",
  "parentSpanId": "zzz",
  "serviceName": "sample-service-a",
  "bizDomain": "iot-home",
  "teamName": "backend-team",
  "env": "test",
  "thread": "main",
  "logger": "xxx",
  "message": "xxx"
}
```

### 8.2 Filebeat 标准配置模板

- 输入类型：log，读取指定目录日志文件；
- 解析方式：JSON 自动解析，无需额外正则；
- 输出目标：OpenSearch，索引按服务+日期拆分；
- 内置字段映射：TraceID、服务名、业务域、团队作为关键字段，支持快速检索与权限控制；
- 权限适配：配合 OpenSearch 文档级权限，实现不同团队仅能查看自身服务日志。

## 9. CI/CD 流水线设计

### 9.1 流水线触发规则

- 分支提交：执行静态检查 + 单元测试；
- Merge Request：执行全量单元测试 + 集成测试；
- 打 Tag 发布：执行全量测试 + 性能测试 + 打包发布。

### 9.2 流水线全流程

```
1. 代码拉取
2. 静态代码检查 (Checkstyle + SpotBugs)
3. 依赖漏洞扫描
4. JDK17 编译构建
5. 单元测试 + 覆盖率校验
6. 集成测试 (Testcontainers)
7. JDK21 兼容性全量测试
8. 性能基准测试 (仅发布分支)
9. 打包制品
10. 发布到内部Maven仓库
11. 生成版本日志与测试报告
```

### 9.3 质量门禁

- 静态检查零阻断级问题；
- 单元测试通过率 100%，覆盖率 ≥ 90%；
- 集成测试通过率 100%；
- JDK17、JDK21 双环境测试全通过；
- 性能损耗在阈值范围内。
