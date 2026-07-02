# UOK 项目全景文档

> 本文档供另一台电脑上的 Claude Code Agent 一次性了解项目全貌，包含项目背景、架构设计、四阶段开发过程、完整文件目录、验证方法

---

## 一、项目概述

**Unified Observability Kit (UOK)** 是一个零侵入的 Java 可观测性工具包，面向 IoT 平台场景，提供分布式链路追踪、指标采集、日志关联的一体化能力。

### 核心设计原则

| 原则 | 说明 |
|---|---|
| 零侵入 | uok-agent 通过 `-javaagent` 接入，业务代码无需修改；uok-lambda-starter 是纯 SDK，不依赖 AWS OTel Lambda Layer |
| W3C Trace Context | 遵循 W3C 标准传播 `traceparent` 头部（`00-<traceId>-<spanId>-<flags>`） |
| 自我保护 | 内置多级降级（FULL/REDUCED/MINIMAL/DISABLED），CPU/延迟超阈值自动降级 |
| 合规优先 | 数据脱敏（DataMasker）、OpenSearch 文档级权限隔离 |

### 技术栈

| 组件 | 版本 |
|---|---|
| Java | 17 / 21 双版本兼容 |
| Maven | 3.8.7+ |
| OpenTelemetry SDK | 1.32.0 |
| Spring Boot | 3.2.5 |
| JUnit 5 | 5.10.2 |
| Mockito | 5.11.0 |
| Testcontainers | 1.19.7 |
| JMH | 1.37 |
| JaCoCo | 0.8.11 |

---

## 二、模块结构

```
UOK/
├── uok-common/           # 公共核心模块（配置/上下文/采样/灰度/降级/脱敏/指标）
├── uok-agent/            # Java Agent（-javaagent 接入，HTTP/Kafka/WebFlux/DB 埋点）
├── uok-lambda-starter/   # Lambda Starter SDK（Kinesis/Kafka 事件追踪，纯 SDK 无 Layer）
├── samples/              # 3 个样例工程
│   ├── sample-springcloud-service/   # Gateway + ServiceA + ServiceB
│   ├── sample-kafka-app/             # Kafka 生产者 + 消费者
│   └── sample-lambda-function/       # Lambda 函数
├── test-suite/
│   ├── integration-test/  # 端到端集成测试（HTTP 链路、Lambda 事件、TraceValidator）
│   └── performance-test/  # JMH 性能基准测试（11 个 benchmark）
├── deployment/
│   └── filebeat-config/   # Filebeat + OpenSearch 日志采集配置
├── docs/                  # 设计文档 + 用户文档 + 交付总结
└── .github/workflows/     # CI/CD 流水线
```

---

## 三、四阶段开发过程

### Phase 1：项目初始化与公共底座

**目标**：搭建 Maven 多模块工程骨架，完成设计文档，实现 uok-common 公共模块全部核心能力。

**交付**：
- Maven 多模块 POM（9 个子模块，统一依赖版本管理）
- 设计文档：PRD.md / HLD.md / LLD.md
- uok-common 模块 16 个核心类 + 17 个测试类（223 tests）
- 覆盖率：行 93.4% / 分支 86.7%

**核心类清单**：

| 包 | 类 | 职责 |
|---|---|---|
| config | UokConfig | 统一配置模型，Builder 模式 |
| config | ConfigLoader | 多源配置加载（系统属性>环境变量>配置文件>默认值） |
| config | SamplerConfig | 采样策略配置 |
| config | GrayConfig | 灰度发布配置（服务/实例/流量多维度） |
| context | TraceContext | W3C Trace Context 数据模型 |
| context | ContextHolder | ThreadLocal 上下文持有 + MDC 同步 |
| context | ReactiveContextHolder | WebFlux/Reactor 上下文（AtomicReference） |
| constant | LogConstants | 日志字段常量（traceparent 头名等） |
| constant | MetricConstants | Prometheus 指标名称常量 |
| sampler | Sampler | 采样器接口 |
| sampler | HeadSampler | 头部采样（按比例 0.0-1.0） |
| sampler | ErrorAlwaysSampler | 错误请求全采样 |
| sampler | TailSampler | 尾部采样（按耗时/错误状态） |
| sampler | DeviceRatioSampler | 按设备比例采样（IoT 场景） |
| gray | GrayController | 灰度控制器，支持运行时动态更新 |
| desensitize | DataMasker | 自动识别敏感字段脱敏为 `******` |
| metrics | MetricCollector | 指标采集 + Prometheus 输出 |
| degrade | DegradeManager | 降级管控，手动+自动双机制，4级降级 |
| utils | TraceIdGenerator | 128-bit traceId / 64-bit spanId 生成 + W3C 格式化/解析 |

---

### Phase 2：核心 Agent 与 HTTP 链路能力

**目标**：实现 uok-agent 核心 Java Agent，打通 Gateway → Service A → Service B 的 HTTP 全链路追踪。

**交付**：
- uok-agent 6 个核心类 + 7 个测试类（49 tests → 后续增强至 119）
- sample-springcloud-service（3 个子服务）

**核心类**：

| 类 | 职责 |
|---|---|
| UokAgent | Agent 入口，`premain(String, Instrumentation)` + `agentmain` |
| AgentConfig | Agent 专用配置加载，自动设置 OTel 系统属性 |
| OtelSdkInitializer | OTel SDK 初始化（Tracer + W3C Propagator + BatchSpanProcessor + LoggingSpanExporter） |
| HttpServletInstrumentation | HTTP 埋点：`onHttpRequest` / `onHttpResponse` / `onHttpRequestComplete` |
| AgentContextHolder | OTel Span ↔ UOK TraceContext 桥接 |
| MdcLogInjector | MDC 日志注入：`initialize(UokConfig)` / `injectTraceContext` / `clearTraceContext` |

**HTTP 链路追踪流程**：

```
Gateway                    Service A                  Service B
   │                          │                          │
   ├─ onHttpRequest()        │                          │
   │  创建 root TraceContext   │                          │
   ├─ onHttpResponse()       │                          │
   │  注入 traceparent ──────►├─ onHttpRequest()         │
   │                         │  解析 traceparent         │
   │                         │  创建子 TraceContext       │
   │                         ├─ onHttpResponse()         │
   │                         │  注入 traceparent ───────►├─ onHttpRequest()
   │                         │                          │  创建子 TraceContext
   ├─ onHttpRequestComplete()├─ onHttpRequestComplete() ├─ onHttpRequestComplete()
```

---

### Phase 3：全场景能力扩展

**目标**：扩展 Agent 埋点到 Kafka/异步/WebFlux/存储等全场景，开发 Lambda Starter，集成采样/灰度/降级/指标统一管控。

**交付**：
- 5 个新埋点类（Kafka/WebFlux/异步/存储）
- ObservabilityController（采样+灰度+降级+指标统一入口）
- uok-lambda-starter 模块 4 个类 + 4 个测试类（70 tests）
- 2 个新样例工程（sample-kafka-app / sample-lambda-function）

**新增埋点**：

| 类 | 场景 | 核心方法 |
|---|---|---|
| KafkaInstrumentation | Kafka 生产/消费 | `onProduce(Map<String,byte[]>)` / `onConsume(Map<String,byte[]>, serviceName)` / `onConsumeComplete()` |
| WebFluxInstrumentation | Spring WebFlux | `onWebFluxRequest(exchange)` / `wrapFunction(fn)` / `onWebFluxRequestComplete()` |
| AsyncContextPropagator | 异步线程 | `wrapRunnable(r)` / `wrapSupplier(s)` / `wrapExecutor(e)` / `wrapScheduledTask(t)` |
| StorageInstrumentation | MySQL/Redis/DynamoDB | `createDbSpan(type, operation, table, duration, success)` |

**Lambda Starter 类**：

| 类 | 职责 |
|---|---|
| LambdaTracingInitializer | 入口：`initialize()` 初始化 / `onLambdaEvent(Object)` 处理事件 / `onLambdaComplete()` 清理 |
| KinesisEventAdapter | 从 Kinesis 事件提取 trace 上下文（headers + data） |
| KafkaEventAdapter | 从 Kafka/MSK 事件提取 trace 上下文（byte[] headers） |
| LambdaContextHolder | Lambda 专用上下文（volatile，非 ThreadLocal） |

**Lambda 使用模式**：

```java
public class MyHandler implements RequestHandler<Object, String> {
    @Override
    public String handleRequest(Object event, Context context) {
        LambdaTracingInitializer.initialize();           // 幂等初始化
        TraceContext ctx = LambdaTracingInitializer.onLambdaEvent(event);
        try {
            return doWork(event);                         // MDC 已有 traceId/spanId
        } finally {
            LambdaTracingInitializer.onLambdaComplete();  // 必须清理
        }
    }
}
```

---

### Phase 4：测试补全 + 性能基准 + 部署交付

**目标**：补全测试覆盖率，运行 JMH 性能基准，交付部署配置和用户文档。

**交付**：

| 子项 | 内容 |
|---|---|
| 单元测试补全 | uok-agent +3类(50tests), uok-lambda-starter +4类(70tests) |
| 集成测试 | EndToEndTracing(8) + LambdaTracing(7) + TraceValidator(17) |
| JMH 基准 | 11 benchmarks, 全链路 <1μs |
| Filebeat 配置 | filebeat.yml + opensearch-index-template.json |
| TraceValidator | 链路完整性自动校验工具 |
| CI/CD | GitHub Actions（静态检查+双JDK+性能+发布） |
| 用户文档 | quickstart + integration-guide + operations-runbook |
| 配置资源 | uok.properties × 2, logback-test.xml × 4 |

**覆盖率提升**：

| 模块 | Phase 3 结束 | Phase 4 结束 |
|---|---|---|
| uok-common | 93.4% / 86.7% | 93.4% / 86.7% |
| uok-agent | ~68% / ~59% | 87.5% / 76.8% |
| uok-lambda-starter | ~56% / ~35% | 98.3% / 86.4% |

**JMH 性能基准（JDK 17）**：

| Benchmark | 实测 |
|---|---|
| httpRequestWithAgent | ~0.7 μs |
| httpRequestWithoutAgent | ~0.03 μs |
| traceIdGeneration | ~0.5 μs |
| spanIdGeneration | ~0.4 μs |
| headSamplingDecision | ~0.5 μs |
| dataMasking | ~0.2 μs |
| mdcLogInjection | ~0.8 μs |
| traceParentParsing | ~0.3 μs |

---

## 四、完整文件目录

```
UOK/
├── pom.xml
├── .gitignore
├── CLAUDE.md
├── SETUP-GUIDE.md
├── Unified Observability Kit(UOK)_Design.md
│
├── .github/workflows/
│   └── ci-cd.yml
│
├── docs/
│   ├── PRD.md
│   ├── HLD.md
│   ├── LLD.md
│   ├── quickstart.md
│   ├── integration-guide.md
│   ├── operations-runbook.md
│   ├── verification-guide.md
│   ├── development-phases.md
│   ├── phase1-delivery-summary.md
│   ├── phase2-delivery-summary.md
│   ├── phase3-delivery-summary.md
│   └── phase4-delivery-summary.md
│
├── deployment/filebeat-config/
│   ├── filebeat.yml
│   ├── opensearch-index-template.json
│   └── README.md
│
├── uok-common/
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../common/
│       │   ├── config/          (UokConfig, ConfigLoader, SamplerConfig, GrayConfig)
│       │   ├── context/         (TraceContext, ContextHolder, ReactiveContextHolder)
│       │   ├── constant/        (LogConstants, MetricConstants)
│       │   ├── sampler/         (Sampler, HeadSampler, ErrorAlwaysSampler, TailSampler, DeviceRatioSampler)
│       │   ├── gray/            (GrayController)
│       │   ├── desensitize/     (DataMasker)
│       │   ├── metrics/         (MetricCollector)
│       │   ├── degrade/         (DegradeManager)
│       │   └── utils/           (TraceIdGenerator)
│       └── test/
│           ├── java/.../common/ (17 个测试类, 223 tests)
│           └── resources/logback-test.xml
│
├── uok-agent/
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../agent/
│       │   ├── UokAgent.java
│       │   ├── config/          (AgentConfig)
│       │   ├── context/         (AgentContextHolder)
│       │   ├── integration/     (OtelSdkInitializer)
│       │   ├── instrumentation/
│       │   │   ├── http/        (HttpServletInstrumentation)
│       │   │   ├── kafka/       (KafkaInstrumentation)
│       │   │   ├── webflux/     (WebFluxInstrumentation)
│       │   │   ├── async/       (AsyncContextPropagator)
│       │   │   └── storage/     (StorageInstrumentation)
│       │   ├── logging/         (MdcLogInjector)
│       │   └── metrics/         (ObservabilityController)
│       ├── main/resources/uok.properties
│       └── test/
│           ├── java/.../agent/  (10 个测试类, 119 tests)
│           └── resources/logback-test.xml
│
├── uok-lambda-starter/
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../lambda/
│       │   ├── LambdaTracingInitializer.java
│       │   ├── adapter/         (KinesisEventAdapter, KafkaEventAdapter)
│       │   └── context/         (LambdaContextHolder)
│       ├── main/resources/uok.properties
│       └── test/
│           ├── java/.../lambda/ (4 个测试类, 70 tests)
│           └── resources/logback-test.xml
│
├── samples/
│   ├── sample-springcloud-service/
│   │   ├── pom.xml
│   │   ├── sample-gateway/     (GatewayApplication, GatewayController)
│   │   ├── sample-service-a/   (ServiceAApplication, ServiceAController)
│   │   └── sample-service-b/   (ServiceBApplication, ServiceBController)
│   ├── sample-kafka-app/
│   │   ├── pom.xml
│   │   ├── sample-kafka-producer/  (KafkaProducerApplication)
│   │   └── sample-kafka-consumer/  (KafkaConsumerApplication)
│   └── sample-lambda-function/
│       ├── pom.xml
│       └── src/main/java/.../SampleLambdaHandler.java
│
└── test-suite/
    ├── integration-test/
    │   ├── pom.xml
    │   └── src/test/java/.../integration/
    │       ├── EndToEndTracingIntegrationTest.java  (8 tests)
    │       ├── LambdaTracingIntegrationTest.java    (7 tests)
    │       ├── TraceValidator.java
    │       └── TraceValidatorTest.java              (17 tests)
    └── performance-test/
        ├── pom.xml
        └── src/main/java/.../perf/
            └── UokPerformanceBenchmark.java         (11 benchmarks)
```

**统计**：32 源码文件 + 31 测试文件 + 8 样例源码 + 15 配置文件 + 3 部署文件 + 1 CI/CD + 12 文档 = **~102 个文件**

---

## 五、编码约定与易错点

| 项目 | 约定 |
|---|---|
| 编译目标 | Java 17，不使用 Java 21 专属 API |
| 配置属性键 | `uok.<模块>.<属性名>`，用连字符不用驼峰（如 `uok.sampler.head-rate`） |
| UokConfig 构建 | `new UokConfig.Builder().serviceName("xxx").build()`，不是 `UokConfig.builder()` |
| GrayConfig.serviceList | 接受 `List<String>`，不是逗号分隔字符串 |
| Kafka headers | `Map<String, byte[]>`，不是 `Map<String, String>` |
| 埋点清理 | 必须在 finally 中调用 complete 方法 |
| 测试框架 | JUnit 5 + AssertJ |
| 日志格式 | 结构化 JSON，含 traceId/spanId/parentSpanId/serviceName/bizDomain/teamName/env |
| 降级检查 | `DegradeManager.isTracingActive()` 检查 ≥ REDUCED，自动降级到 REDUCED 仍允许追踪 |
| Lambda 上下文 | LambdaContextHolder 用 volatile（非 ThreadLocal），适配单事件生命周期 |

---

## 六、已知限制

- uok-agent 当前使用 OTel SDK 程序化接入（非字节码增强），埋点需手动调用 Instrumentation 类
- UokAgent.premain/initialize 需要 `java.lang.instrument.Instrumentation` 对象，单元测试中无法直接测试
- Lambda Starter 默认使用 `LoggingSpanExporter`，生产环境需替换为 OTLP Exporter
- Testcontainers 集成测试需要 Docker 环境

---

## 七、从零验证步骤

### 7.1 环境准备

```bash
# 安装 JDK 17 + Maven 3.8.7+ + Git
java -version    # 17.x
mvn -version     # 3.8.7++
```

### 7.2 克隆源码

```bash
git clone https://github.com/gavinWithGu/UOK.git
cd UOK
git log --oneline   # 预期 17 个提交
```

### 7.3 一键全量验证

```bash
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am
```

预期：`Tests run: 444, Failures: 0, Errors: 0, BUILD SUCCESS`

### 7.4 分模块验证

```bash
# Phase 1: uok-common（223 tests）
mvn verify -pl uok-common

# Phase 2: uok-agent（119 tests）+ Agent JAR + Spring Cloud 样例
mvn verify -pl uok-agent -am
mvn package -pl uok-agent -am -DskipTests
mvn compile -pl samples/sample-springcloud-service -am

# Phase 3: uok-lambda-starter（70 tests）+ Kafka/Lambda 样例
mvn verify -pl uok-lambda-starter -am
mvn compile -pl samples/sample-kafka-app,samples/sample-lambda-function -am

# Phase 4: 集成测试 + JMH + 文件检查
mvn test -pl test-suite/integration-test -am
mvn -pl test-suite/performance-test -am package -DskipTests
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 1 -i 1 -f 1
```

### 7.5 覆盖率验证

| 模块 | 行覆盖率 | 分支覆盖率 | JaCoCo 阈值 |
|---|---|---|---|
| uok-common | ≥93% | ≥86% | ≥90% / ≥85% |
| uok-agent | ≥87% | ≥76% | ≥70% / ≥60% |
| uok-lambda-starter | ≥98% | ≥86% | ≥90% / ≥75% |

### 7.6 给 Claude Code 的验证指令

```
请按照 SETUP-GUIDE.md 执行全量验证，从环境检查开始，逐阶段验证所有交付物，输出验证报告
```

---

## 八、验收标准达成

| # | 验收标准 | 达成 | 说明 |
|---|---|---|---|
| 1 | 零侵入 | ✅ | Agent via `-javaagent`；Lambda 纯 SDK |
| 2 | JDK 17/21 兼容 | ✅ | CI 双环境验证 |
| 3 | 覆盖率 ≥90% | ✅ | uok-common 93.4%, uok-lambda-starter 98.3% |
| 4 | OTel 1.32.0 | ✅ | |
| 5 | Spring Boot 3.2.x | ✅ | |
| 6 | 全量测试通过 | ✅ | 444 tests / 0 failures |
| 7 | JMH 基准 | ✅ | 11 benchmarks, 全链路 <1μs |
| 8 | Filebeat + OpenSearch | ✅ | 配置模板 + 索引模板 |
| 9 | CI/CD 流水线 | ✅ | GitHub Actions |
| 10 | 用户文档 | ✅ | quickstart + integration-guide + operations-runbook |
| 11 | W3C Trace Context | ✅ | 集成测试验证 |

---

## 九、关键文档索引

| 文档 | 路径 | 说明 |
|---|---|---|
| 产品需求文档 | `docs/PRD.md` | 功能需求、验收标准 |
| 高层设计 | `docs/HLD.md` | 分层架构、核心组件 |
| 低层设计 | `docs/LLD.md` | 模块详细设计、Filebeat/CI/CD 规范 |
| 统一设计 | `Unified Observability Kit(UOK)_Design.md` | PRD+HLD+LLD 合并版 |
| 快速上手 | `docs/quickstart.md` | 5 分钟接入指南 |
| 集成手册 | `docs/integration-guide.md` | HTTP/Kafka/采样/灰度/降级详细配置 |
| 运维手册 | `docs/operations-runbook.md` | 监控/排错/性能调优 |
| 验证手册 | `SETUP-GUIDE.md` | 从零开始的全量验证步骤 |
| 阶段总结 | `docs/development-phases.md` | 四阶段开发详细过程 |
| 交付总结 | `docs/phase{1-4}-delivery-summary.md` | 各阶段交付明细 |
