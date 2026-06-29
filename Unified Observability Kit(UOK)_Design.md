# Unified Observability Kit \(UOK\) 完整技术设计文档

> 版本：V1\.1 正式版 \| 适用场景：IoT 平台全链路统一观测 \| 兼容环境：OpenJDK 17/21、Spring Cloud、AWS Lambda、Kafka/MSK
> 用途：直接用于 Claude Code \+ Superpowers 原型工程开发，覆盖需求、架构、实现三层设计，包含全量测试与验证体系
> 
> 

---

## 一、需求规格说明书 \(PRD\)

### 1\.1 项目背景与目标

#### 背景

当前 IoT 平台上下行链路日志分散在 CloudWatch、多套 OpenSearch 中，故障定位需跨系统切换，MTTR 居高不下；同时现有埋点方案依赖 AWS 专属组件，厂商绑定度高，业务团队接入成本高、改造量大。

#### 核心目标

1. 打造架构组统一维护的观测产品，业务团队零代码接入即可实现全链路 Trace 透传与日志关联；

2. 彻底去除 AWS 厂商绑定，全栈采用开源 OpenTelemetry 体系，容器化 / 本地化均可部署；

3. 配套完整的自动化测试与性能观测体系，保障接入后业务稳定性与性能损耗可控；

4. 输出标准格式日志，无缝对接 Filebeat → OpenSearch 采集链路，支撑统一日志查询；

5. 覆盖 IoT \+ 微服务全场景通用需求，支持多团队协作、多环境适配、灰度上线与合规隔离。

### 1\.2 核心功能需求

|需求分类|需求项|详细描述|
|---|---|---|
|核心接入能力|Java Agent 零侵入接入|后端微服务仅需添加 `-javaagent` 启动参数，无需修改任何业务代码，自动完成全链路埋点|
||Lambda Starter 零侵入接入|Java 语言 Lambda 仅需引入 Maven 依赖，无需修改业务代码，自动完成链路上下文提取与续传；不依赖 AWS 官方 OTel Layer，纯开源 SDK 封装|
|链路透传能力|HTTP 调用透传|自动拦截 Spring MVC、Spring Cloud Gateway、RestTemplate、Feign 调用，自动注入 / 提取 Trace 上下文|
||Kafka/MSK 消息透传|自动拦截 Kafka 生产者 / 消费者，通过消息 Header 透传 Trace 上下文，不修改业务 Payload|
||**全异步场景透传**|自动适配线程池、`CompletableFuture`、Spring `@Async`、定时任务（`@Scheduled`）、响应式编程（Spring WebFlux/Reactor），所有异步场景 TraceID 不丢失，业务代码零感知|
||数据库与缓存调用埋点|自动拦截 MySQL JDBC、DynamoDB SDK、Redis \(Jedis/Lettuce\) 调用，生成独立 Span，记录 SQL / 命令、耗时，支持慢查询溯源|
|日志关联能力|日志自动注入|自动适配 Log4j2、Logback，将 TraceID、SpanID、ParentSpanID、服务名注入日志上下文|
||**多维度业务字段注入**|支持设备 ID、用户 ID、故障码、订单号等业务字段自动注入 MDC，无需业务代码手动处理，支持按业务维度检索|
||标准日志格式|输出结构化可解析日志，支持 Filebeat 直接采集解析|
|采样策略能力|**多级采样体系**|支持头部采样、尾部采样、错误日志 100% 全采；针对 IoT 高流量场景，支持按设备 ID 比例采样，灵活平衡排查能力与存储成本|
|配置管理能力|统一配置项|支持服务名、业务域、团队、环境、采样率、输出目标、脱敏规则、灰度规则等配置统一管理|
||**多环境自动适配**|自动识别 dev/test/prod 环境，差异化配置采样率、日志输出目标、埋点强度，配置集中管理|
||**灰度接入能力**|支持按服务名、实例 IP、实例标签、流量比例灰度开启埋点，先试点再全量，降低上线风险|
|性能与可靠性|**性能开销管控**|Agent 字节码增强 CPU 开销 ≤ 5%，内存开销 ≤ 100MB；内置手动 \+ 自动双重降级开关，极端异常场景自动熔断关闭埋点，不影响业务主流程|
||错误链路全量捕获|异常堆栈自动关联 TraceID，错误日志强制全采样，正常日志可按比例采样，兼顾排查能力与存储成本|
|扩展能力|**可观测性三合一**|除日志与链路外，同步自动采集服务黄金指标（QPS、平均耗时、错误率、P95/P99 耗时），原生支持 Prometheus 格式输出，未来可无缝对接 Grafana，实现日志、链路、指标一体化|
|合规与权限|**多团队权限隔离支撑**|日志自动携带服务、业务域、团队标签字段，支撑 OpenSearch 按字段做数据权限隔离，可对接企业 SSO，满足多团队数据隔离合规要求|

### 1\.3 非功能需求

1. **JDK 兼容性**：100% 兼容 OpenJDK 17、OpenJDK 21，支持 Spring Boot 2\.7\.x/ 3\.x、Spring Cloud 2021\+、Spring WebFlux

2. **性能损耗**：正常流量下，Agent 接入后 CPU 开销 ≤ 5%，内存开销 ≤ 100MB，请求响应时延增加 ≤ 3%；内置自动降级机制

3. **可靠性**：Agent 异常不影响业务主流程，具备手动 \+ 自动双重降级开关，故障时自动关闭埋点

4. **可测试性**：单元测试覆盖率 ≥ 90%，集成测试覆盖所有核心透传场景，性能指标可量化观测

5. **可维护性**：架构组统一维护版本，业务团队无感升级，支持按服务 / 实例灰度发布与快速回滚

6. **合规性**：支持多团队数据权限隔离，内置敏感字段脱敏，满足数据安全合规要求

7. **去云绑定**：核心能力无 AWS 专属依赖，可平滑迁移至自建容器环境、其他云平台

### 1\.4 交付物清单

|交付物类型|交付物名称|说明|
|---|---|---|
|核心制品|`uok-agent.jar`|适用于所有常驻 JVM 微服务的 Java Agent 包|
||`uok-lambda-starter.jar`|适用于 AWS Lambda Java 函数的启动器依赖包|
||`uok-common.jar`|公共核心模块，被上述两个制品依赖|
|样例工程|`sample-springcloud-service`|Spring Cloud 微服务样例，包含网关 \+ 两个业务服务，验证跨服务 HTTP 调用透传|
||`sample-kafka-app`|Kafka 生产者 \+ 消费者样例，验证消息链路透传|
||`sample-lambda-function`|AWS Lambda Java 函数样例，验证 Kinesis 触发场景下的链路提取与续传|
|测试套件|单元测试用例集|覆盖所有核心模块与工具类|
||自动化集成测试套件|基于 Testcontainers 模拟真实业务场景|
||性能基准测试套件|量化性能损耗，输出性能报告|
|对接配置|Filebeat 采集配置模板|标准采集配置，直接对接 OpenSearch|
|文档|接入手册、运维手册、API 文档|面向业务团队与运维团队|

### 1\.5 验收标准

1. 微服务引入 Agent 后，一次跨服务调用中，所有服务日志包含相同 TraceID、不同 SpanID、正确的 ParentSpanID，形成完整调用树；

2. Kafka 生产者发送的消息，消费者端可自动提取 TraceID 并续传链路，日志链路连续，消息 Payload 无修改；

3. Lambda 函数引入 Starter 后，可从 Kinesis 事件中提取 TraceID，函数日志携带完整链路信息，无 AWS Layer 依赖；

4. 异步线程、定时任务、WebFlux 响应式场景下，TraceID 全程不丢失；

5. MySQL、DynamoDB、Redis 调用自动生成独立 Span，可记录操作内容与耗时；

6. 异常场景错误日志 100% 采样，自动关联 TraceID，正常日志可按配置比例采样；

7. 支持按设备 ID、用户 ID 等业务字段检索日志，字段自动注入无需业务代码；

8. 所有场景下，业务代码零修改，仅通过启动参数或依赖引入即可生效；

9. 性能测试报告显示，接入后业务服务 QPS 下降 ≤ 5%，平均响应时延增加 ≤ 3%；

10. 单元测试覆盖率 ≥ 90%，所有集成测试用例全量通过；

11. 日志格式可被 Filebeat 直接解析，成功写入 OpenSearch，支持按服务、业务域做权限隔离。

---

## 二、高层设计文档 \(HLD\)

### 2\.1 整体架构设计

采用**分层解耦架构**，自下而上分为六层，所有层均为纯开源实现，无云厂商绑定，完整覆盖日志、链路、指标三类可观测能力：

```Plain Text
┌─────────────────────────────────────────────────────────┐
│                  业务接入层 (零代码)                     │
│ Spring Cloud服务  Kafka服务  Lambda函数  网关  WebFlux   │
└─────────────────────┬───────────────────────────────────┘
                      │ 无侵入挂载/引入
┌─────────────────────▼───────────────────────────────────┐
│                  UOK 核心能力层                          │
│  uok-agent (字节码增强)   uok-lambda-starter (SDK封装)   │
│  ────────────────────────────────────────────────────   │
│ 链路生成  上下文透传  日志注入  多级采样  脱敏处理        │
│ 异步透传  存储埋点  性能管控  灰度控制  指标采集        │
└─────────────────────┬───────────────────────────────────┘
                      │ 标准格式输出
┌─────────────────────▼───────────────────────────────────┐
│                  可观测输出层                            │
│      结构化日志文件      链路数据      Prometheus指标     │
└─────────────────────┬───────────────────────────────────┘
                      │ 采集对接
┌─────────────────────▼───────────────────────────────────┐
│                  采集对接层                              │
│  Filebeat采集 → OpenSearch (权限隔离+多维度检索)         │
│  Prometheus采集 → Grafana 指标可视化                     │
└─────────────────────┬───────────────────────────────────┘
                      │ 验证观测
┌─────────────────────▼───────────────────────────────────┐
│                  测试观测层                              │
│  单元测试  集成测试  性能基准测试  链路正确性校验         │
└─────────────────────────────────────────────────────────┘
```

### 2\.2 核心组件职责

1. **uok\-common**：公共基础模块，封装 Trace 上下文模型、配置解析、日志字段常量、脱敏工具、采样策略、灰度控制、降级管理、指标采集，被两个核心制品复用，保障能力一致性。

2. **uok\-agent**：基于 OpenTelemetry Java Agent 二次封装，通过字节码增强实现对所有 JVM 服务的无侵入埋点、上下文透传、日志注入、指标采集，是常驻服务的核心接入方式。

3. **uok\-lambda\-starter**：基于 OpenTelemetry SDK 封装的 Lambda 专用启动器，自动初始化观测上下文，支持从 Kinesis/MSK 事件中提取 TraceID，适配函数计算的短生命周期场景，无任何 AWS 运行时依赖。

4. **样例工程集**：覆盖三类典型业务场景，用于功能验证、集成测试、接入演示，同时作为自动化测试的验证载体。

### 2\.3 全链路 Trace 透传核心模型

采用标准 W3C Trace Context 规范，通过 `TraceID + SpanID + ParentSpanID` 三级模型构建完整调用树，天然支持父服务调用多微服务、并行调用、消息分发等场景：

- **TraceID**：全局唯一，一次完整业务请求全链路共用，128 位随机数；

- **SpanID**：每个独立操作单元（服务调用、消息发送、数据库操作、异步任务）唯一，64 位随机数；

- **ParentSpanID**：标记当前操作的父级节点，根节点 ParentSpanID 为空。

#### 典型场景自动处理逻辑

|场景|自动处理逻辑|
|---|---|
|串行调用多下游|父服务生成主 Span，每调用一个下游生成一个子 Span，子 Span 的 ParentSpanID 指向父 Span，形成链式结构|
|并行调用多微服务|OTel Agent 自动包装线程池 / 响应式上下文，上下文自动传递，每个并行任务生成独立子 Span，共同指向同一个父 Span|
|消息队列一对多消费|生产端生成一个发送 Span，每个消费端各自生成消费 Span，ParentSpanID 都指向发送 Span，实现一对多消息链路追踪|
|异步 / 定时任务|自动捕获父上下文，任务执行时自动续传，生成独立子 Span|

### 2\.4 核心能力专项设计

#### 2\.4\.1 多级采样体系设计

采用「头部采样 \+ 尾部兜底 \+ 错误强制全采」三级策略，IoT 场景扩展按设备维度采样：

1. **头部采样**：链路入口处按配置比例决定是否全链路采样，降低全链路处理开销；

2. **错误全采**：只要链路中出现异常 / 错误日志，强制提升为全采样，确保故障排查数据完整；

3. **尾部采样**：支持按耗时阈值、异常特征做尾部兜底采样，保留慢请求与异常请求全量数据；

4. **IoT 专属：按设备比例采样**：支持配置设备 ID 哈希采样比例，针对海量设备上报场景，只采集部分设备全量链路，大幅降低存储成本，同时保留问题排查能力。

#### 2\.4\.2 灰度接入机制设计

支持多维度灰度控制，所有规则通过配置下发，无需重启服务：

1. **按服务灰度**：指定服务名开启 / 关闭埋点；

2. **按实例灰度**：按实例 IP、实例标签、实例数量比例灰度；

3. **按流量灰度**：按请求比例灰度开启链路采样。

#### 2\.4\.3 性能管控与自动降级设计

1. **内置性能探针**：Agent 自身监控 CPU、内存占用，以及业务请求时延增量；

2. **双降级机制**：

    - 手动降级：通过配置全局 / 服务级关闭埋点；

    - 自动降级：当监测到 CPU 占用超过阈值、业务时延增幅超过阈值时，自动关闭链路埋点能力，仅保留基础日志注入，极端情况完全卸载字节码增强，保障业务可用性。

#### 2\.4\.4 可观测性三合一设计

一次接入，同时输出三类可观测数据，避免多套 Agent 叠加开销：

1. **日志（Logging）**：结构化日志文件，携带 TraceID 与业务字段，对接 OpenSearch；

2. **链路（Tracing）**：标准 OTel 链路数据，可对接 Zipkin/Jaeger，用于故障根因定位；

3. **指标（Metrics）**：自动生成服务黄金指标（QPS、错误率、各分位耗时），原生支持 Prometheus 格式暴露，直接对接 Grafana。

### 2\.5 测试与性能观测体系架构

采用**三层测试防护体系**，从代码质量到场景验证再到性能量化全链路覆盖：

1. **单元测试层**：覆盖公共模块所有工具类、配置解析、上下文模型，保障基础逻辑正确性；

2. **集成测试层**：基于 Testcontainers 启动真实服务与中间件，模拟完整业务调用链路，自动化校验 TraceID 透传连续性、日志字段完整性、采样 / 降级 / 灰度规则有效性；

3. **性能基准层**：基于 JMH 构建基准测试，对比「无 Agent」与「接入 Agent」两组基准数据，量化 CPU、内存、QPS、时延四项核心指标，输出可视化性能报告。

### 2\.6 日志采集对接架构

遵循**业务侧零改造、采集侧标准适配**原则：

1. UOK 自动将链路字段、权限标签字段注入日志上下文，业务日志按标准 JSON 格式输出到本地文件；

2. 部署 Filebeat 作为采集端，读取日志文件，自动解析所有结构化字段；

3. Filebeat 直接转发至 OpenSearch，支撑统一查询、链路检索与字段级权限隔离；

4. 全程无需修改业务日志配置，仅需替换标准 Filebeat 配置文件即可完成对接。

---

## 三、低层设计文档 \(LLD\)

### 3\.1 项目整体模块划分与目录结构

```Plain Text
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

**构建工具**：Maven 3\.8\+
**编译版本**：源 / 目标版本均为 Java 17，同步验证 Java 21 兼容性
**核心依赖版本**：

- OpenTelemetry: 1\.32\.0

- Spring Boot: 3\.2\.x（兼容 2\.7\.x）

- Log4j2: 2\.20\.x

- Testcontainers: 1\.19\.x

- JMH: 1\.37

### 3\.2 uok\-common 公共核心模块详细设计

#### 3\.2\.1 核心包结构

```Plain Text
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

#### 3\.2\.2 核心配置项

|配置 Key|类型|默认值|说明|
|---|---|---|---|
|`uok.service.name`|String|`unknown-service`|服务名称|
|`uok.biz.domain`|String|`default`|业务域，用于权限隔离|
|`uok.team.name`|String|`default`|所属团队，用于权限隔离|
|`uok.env`|String|自动识别|环境标识，自动识别 dev/test/prod|
|`uok.agent.enabled`|Boolean|`true`|总开关，false 则完全关闭埋点|
|`uok.log.enable`|Boolean|`true`|是否开启日志注入|
|`uok.trace.enable`|Boolean|`true`|是否开启链路追踪|
|`uok.metrics.enable`|Boolean|`true`|是否开启指标采集|
|`uok.sampler.head-rate`|Double|`1.0`|头部采样率|
|`uok.sampler.error-always`|Boolean|`true`|错误日志是否强制全采|
|`uok.sampler.device-ratio`|Double|`1.0`|按设备比例采样率（IoT 场景）|
|`uok.degrade.cpu-threshold`|Integer|`80`|CPU 阈值（%），超阈值自动降级|
|`uok.degrade.latency-increase`|Integer|`10`|时延增幅阈值（%），超阈值自动降级|
|`uok.gray.service-list`|List|空|灰度开启的服务列表|
|`uok.gray.instance-ratio`|Double|`1.0`|实例灰度比例|

### 3\.3 uok\-agent 模块详细设计

#### 3\.3\.1 核心能力封装

基于 OpenTelemetry Java Agent 进行二次封装，内置所有常用插件（Spring Web、Spring Cloud、Kafka、JDBC、Redis、DynamoDB、线程池、WebFlux），业务侧无需单独引入任何插件。

#### 3\.3\.2 字节码增强覆盖范围

|增强点|实现方式|核心逻辑|
|---|---|---|
|HTTP 入口|Servlet 字节码增强|提取请求头 Trace 上下文，无则生成根上下文，注入 MDC|
|HTTP 出口|RestTemplate/Feign/WebClient 字节码增强|将当前 Trace 上下文写入请求头，生成出口 Span|
|响应式入口|WebFlux 字节码增强|适配 Reactor 上下文，响应式请求链路 TraceID 不丢失|
|Kafka 生产|Kafka Producer 字节码增强|将 Trace 上下文写入消息 Header，生成发送 Span|
|Kafka 消费|Kafka Consumer 字节码增强|从消息 Header 提取上下文，生成消费 Span，注入 MDC|
|异步线程|线程池 / CompletableFuture/@Async 字节码增强|上下文跨线程传递，异步日志携带 TraceID|
|定时任务|@Scheduled 字节码增强|定时任务自动生成独立 Trace，执行日志携带 TraceID|
|MySQL 操作|JDBC 驱动字节码增强|生成数据库操作 Span，记录 SQL 与耗时|
|DynamoDB 操作|AWS SDK 字节码增强|拦截 DynamoDB 读写请求，生成 Span，记录操作类型与耗时|
|Redis 操作|Jedis/Lettuce 字节码增强|拦截 Redis 命令，生成 Span，记录命令与耗时|
|异常捕获|全局异常字节码增强|异常堆栈自动关联 TraceID，触发错误全采策略|

#### 3\.3\.3 日志注入与多维度字段逻辑

- 自动注入基础字段：`traceId`、`spanId`、`parentSpanId`、`serviceName`、`env`、`bizDomain`、`teamName`

- 业务字段自动扩展机制：

    - 可通过配置指定 HTTP 请求头、Kafka 消息 Header 中的业务字段（如 `device-id`、`user-id`），自动提取并注入 MDC；

    - 支持自定义扩展接口，业务可按需注册字段提取器，零侵入注入业务维度。

#### 3\.3\.4 指标自动采集逻辑

- 自动统计每个接口 / 方法的：请求量、成功量、失败量、平均耗时、P50/P95/P99 耗时、错误率；

- 标准 Prometheus 格式暴露，端口可配置；

- 指标自动携带服务名、环境、接口名标签，支持多维度聚合。

### 3\.4 uok\-lambda\-starter 模块详细设计

#### 3\.4\.1 设计原则

纯 SDK 封装，不依赖 AWS 官方 OTel Layer，无任何 AWS 专属运行时依赖，仅依赖 AWS Lambda Java Core SDK 与 OpenTelemetry 开源 SDK，可在任意兼容 Java 的函数环境运行。

#### 3\.4\.2 核心包结构

```Plain Text
com.bosch.iot.uok.lambda
├── LambdaTracingInitializer.java    # 自动初始化入口
├── adapter/
│   ├── KinesisEventAdapter.java     # Kinesis事件Trace提取
│   └── KafkaEventAdapter.java       # MSK事件Trace提取
└── context/
    └── LambdaContextHolder.java
```

#### 3\.4\.3 核心逻辑

1. 函数启动时自动初始化 OTel SDK，加载配置；

2. 通过事件适配器，从 Kinesis/MSK 事件记录中提取 Trace 上下文；

3. 若事件中无 TraceID，则自动生成新的根 Trace；

4. 将 Trace 上下文注入日志 MDC，函数内所有日志自动携带链路字段；

5. 函数内调用下游 HTTP/Kafka 时，自动续传链路上下文。

### 3\.5 样例工程详细设计

#### 3\.5\.1 sample\-springcloud\-service

- **组成**：Spring Cloud Gateway \+ Service A \+ Service B

- **验证场景**：Gateway → Service A → Service B 三级 HTTP 调用，包含异步线程、数据库调用、Redis 调用

- **验证点**：

    1. 全链路 TraceID 一致；

    2. 每个服务 SpanID 不同，ParentSpanID 正确指向上游；

    3. 所有服务日志均携带完整链路字段与权限字段；

    4. 服务内异步线程、定时任务日志 TraceID 不丢失；

    5. MySQL、Redis 调用生成独立 Span。

#### 3\.5\.2 sample\-kafka\-app

- **组成**：Kafka Producer 应用 \+ Kafka Consumer 应用

- **验证场景**：生产者发送消息 → 消费者接收并异步处理

- **验证点**：

    1. 生产端与消费端 TraceID 一致；

    2. 消费端 Span 的 ParentSpanID 指向生产端发送 Span；

    3. 消息 Body 未被修改，仅 Header 携带上下文；

    4. 消费者异步处理链路不中断。

#### 3\.5\.3 sample\-lambda\-function

- **组成**：基于 Kinesis 触发的 Java Lambda 函数

- **验证场景**：Kinesis 流推送事件 → Lambda 消费处理 → 调用下游 HTTP 接口

- **验证点**：

    1. 可从 Kinesis 记录中提取 TraceID；

    2. Lambda 函数日志携带正确的 TraceID、SpanID；

    3. 函数内调用下游接口时链路自动续传；

    4. 无 AWS OTel Layer 依赖，纯 SDK 方式运行。

### 3\.6 测试体系详细设计

#### 3\.6\.1 单元测试

- **覆盖范围**：uok\-common 所有类、配置解析、上下文模型、采样器、脱敏工具、灰度控制器、降级管理器

- **技术栈**：JUnit 5 \+ Mockito

- **卡点要求**：行覆盖率 ≥ 90%，分支覆盖率 ≥ 85%

- **执行时机**：每次代码提交自动执行

#### 3\.6\.2 自动化集成测试

- **技术方案**：JUnit 5 \+ Testcontainers

- **测试用例清单**：

    1. Spring Cloud 同步跨服务调用链路正确性测试

    2. Spring WebFlux 响应式链路透传测试

    3. Kafka 生产消费链路透传测试

    4. 线程池 /@Async 异步线程 TraceID 传递测试

    5. 定时任务 Trace 生成测试

    6. MySQL/DynamoDB/Redis 存储操作埋点测试

    7. 日志字段注入正确性测试（含业务字段与权限字段）

    8. Lambda 事件提取与链路生成测试

    9. 多级采样策略生效测试（头部采样 / 错误全采 / 按设备采样）

    10. 灰度接入规则生效测试

    11. 自动降级触发与恢复测试

    12. Prometheus 指标输出正确性测试

- **验证方法**：自动解析日志文件，校验 TraceID、SpanID、ParentSpanID 的存在性与层级关系，自动断言链路完整性。

#### 3\.6\.3 性能基准测试

- **技术方案**：JMH \(Java Microbenchmark Harness\)

- **测试维度**：

    1. QPS 对比：无 Agent vs 接入 Agent 的接口吞吐量

    2. 平均响应时延对比

    3. CPU 使用率对比

    4. 内存占用对比

    5. 高并发下稳定性测试

- **输出产物**：自动化生成性能对比报告，量化损耗百分比，超出阈值自动告警。

### 3\.7 链路验证方案设计

#### 3\.7\.1 日志输出验证

所有样例工程均配置 Log4j2 输出结构化 JSON 日志文件，验证脚本自动扫描日志文件：

1. 校验每条日志均包含 `traceId`、`spanId` 字段；

2. 同一次请求的所有日志 `traceId` 完全一致；

3. 调用链路上游的 `spanId` 等于下游的 `parentSpanId`；

4. 根节点日志的 `parentSpanId` 为空或特定标识；

5. 错误场景日志 100% 保留，正常场景符合采样比例。

#### 3\.7\.2 自动化校验工具

提供 `TraceValidator` 工具类，集成在测试套件中，可自动解析日志文件，输出链路拓扑与校验结果，支持断言集成到测试用例中。

### 3\.8 Filebeat 对接规范

#### 3\.8\.1 日志格式约定

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

#### 3\.8\.2 Filebeat 标准配置模板

- 输入类型：log，读取指定目录日志文件；

- 解析方式：JSON 自动解析，无需额外正则；

- 输出目标：OpenSearch，索引按服务 \+ 日期拆分；

- 内置字段映射：TraceID、服务名、业务域、团队作为关键字段，支持快速检索与权限控制；

- 权限适配：配合 OpenSearch 文档级权限，实现不同团队仅能查看自身服务日志。

### 3\.9 CI/CD 流水线设计

#### 3\.9\.1 流水线触发规则

- 分支提交：执行静态检查 \+ 单元测试；

- Merge Request：执行全量单元测试 \+ 集成测试；

- 打 Tag 发布：执行全量测试 \+ 性能测试 \+ 打包发布。

#### 3\.9\.2 流水线全流程

```Plain Text
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

#### 3\.9\.3 质量门禁

- 静态检查零阻断级问题；

- 单元测试通过率 100%，覆盖率 ≥ 90%；

- 集成测试通过率 100%；

- JDK17、JDK21 双环境测试全通过；

- 性能损耗在阈值范围内。

---

## 四、开发执行顺序建议

1. 搭建 `uok-common` 公共模块，完成单元测试与基础能力验证；

2. 开发 `uok-agent` 核心 HTTP 链路能力，同步搭建 Spring Cloud 样例，验证跨服务透传与日志注入；

3. 扩展异步场景、Kafka、数据库 / 缓存埋点能力，完善对应样例工程；

4. 开发 `uok-lambda-starter`，搭建 Lambda 样例工程；

5. 完善采样、灰度、降级、指标等扩展能力；

6. 补全全量集成测试与性能测试套件；

7. 输出 Filebeat 配置模板，完成 OpenSearch 对接验证；

8. 搭建 CI/CD 流水线，实现全流程自动化。

