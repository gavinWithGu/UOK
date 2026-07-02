# CLAUDE.md — Unified Observability Kit (UOK)

## 项目概述

UOK 是一个**零侵入**的 Java 可观测性工具包，面向 IoT 平台场景，提供分布式链路追踪、指标采集、日志关联的一体化能力。

核心设计原则：
- **零侵入**：uok-agent 通过 `-javaagent` 接入，业务代码无需任何修改；uok-lambda-starter 是纯 SDK，不依赖 AWS OTel Lambda Layer
- **W3C Trace Context**：遵循 W3C 标准传播 `traceparent` 头部（`00-<traceId>-<spanId>-<flags>`）
- **自我保护**：内置多级降级（FULL/REDUCED/MINIMAL/DISABLED），CPU/延迟超阈值自动降级
- **合规优先**：数据脱敏（DataMasker）、OpenSearch 文档级权限隔离

## 技术栈

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

## 模块结构

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

## 核心类速查

### uok-common（16 个类）
- `UokConfig` — 统一配置模型，用 `new UokConfig.Builder()` 构建（不是静态 `builder()` 方法）
- `ConfigLoader` — 多源配置加载（系统属性 > 环境变量 > properties 文件 > 默认值），属性键用连字符如 `uok.sampler.head-rate`
- `TraceContext` — W3C Trace Context 数据模型
- `ContextHolder` — ThreadLocal 上下文持有 + MDC 同步
- `ReactiveContextHolder` — WebFlux/Reactor 上下文（AtomicReference）
- `DegradeManager` — 降级管控，手动 + 自动双机制，`isTracingActive()` 检查 ≥ REDUCED 级别
- `GrayController` — 灰度控制，支持服务/实例IP/标签/比例
- `DataMasker` — 自动识别敏感字段（password/token/secret 等）脱敏为 `******`
- `HeadSampler / ErrorAlwaysSampler / TailSampler / DeviceRatioSampler` — 四种采样策略

### uok-agent（11 个类）
- `UokAgent` — Agent 入口，`premain(String, Instrumentation)` 方法
- `HttpServletInstrumentation` — HTTP 埋点三件套：`onHttpRequest` / `onHttpResponse` / `onHttpRequestComplete`
- `KafkaInstrumentation` — Kafka 埋点：`onProduce(Map<String,byte[]>)` / `onConsume(Map<String,byte[]>, serviceName)` / `onConsumeComplete()`
- `WebFluxInstrumentation` — WebFlux 埋点，配合 `ReactiveContextHolder`
- `AsyncContextPropagator` — 异步线程透传，`wrapRunnable` / `wrapSupplier` / `wrapExecutor`
- `StorageInstrumentation` — MySQL/Redis/DynamoDB 操作 Span
- `ObservabilityController` — 采样+灰度+降级+指标统一入口
- `AgentContextHolder` — OTel Span ↔ UOK TraceContext 桥接
- `MdcLogInjector` — MDC 日志注入，`initialize(UokConfig)` / `injectTraceContext` / `clearTraceContext`
- `OtelSdkInitializer` — OTel SDK 初始化（W3C propagator + BatchSpanProcessor）

### uok-lambda-starter（4 个类）
- `LambdaTracingInitializer` — Lambda 追踪入口：`initialize()` / `onLambdaEvent(Object)` / `onLambdaComplete()`
- `KinesisEventAdapter` — 从 Kinesis 事件提取 trace 上下文
- `KafkaEventAdapter` — 从 Kafka/MSK 事件提取 trace 上下文（byte[] headers）
- `LambdaContextHolder` — Lambda 单事件生命周期上下文（volatile，非 ThreadLocal）

## 构建与验证

```bash
# 全量构建 + 测试 + 覆盖率校验（最常用）
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am

# 只运行测试（跳过覆盖率）
mvn test -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am

# JMH 性能基准
mvn -pl test-suite/performance-test -am package -DskipTests
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 1 -i 1 -f 1

# 样例工程编译
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am
```

## 覆盖率阈值

| 模块 | 行覆盖率 | 分支覆盖率 |
|---|---|---|
| uok-common | ≥90% | ≥85% |
| uok-agent | ≥70% | ≥60% |
| uok-lambda-starter | ≥90% | ≥75% |

## 编码约定

- Java 17 为编译目标，不使用 Java 21 专属 API
- 配置属性键格式：`uok.<模块>.<属性名>`，用连字符不用驼峰（如 `uok.sampler.head-rate`）
- UokConfig 构建：`new UokConfig.Builder().serviceName("xxx").build()`，不是 `UokConfig.builder()`
- GrayConfig 的 `setServiceList` 接受 `List<String>`，不是逗号分隔字符串
- Kafka headers 类型为 `Map<String, byte[]>`，不是 `Map<String, String>`
- 所有埋点方法必须在 finally 中调用 complete/cleanup 方法（`onHttpRequestComplete` / `onConsumeComplete` / `onLambdaComplete`）
- 测试使用 JUnit 5 + AssertJ，不用 JUnit 4
- 日志格式：结构化 JSON，字段包含 traceId/spanId/parentSpanId/serviceName/bizDomain/teamName/env

## 已知限制

- uok-agent 当前使用 OTel SDK 程序化接入（非字节码增强），埋点需手动调用 Instrumentation 类
- UokAgent.premain/initialize 需要 `java.lang.instrument.Instrumentation` 对象，单元测试中无法直接测试
- Lambda Starter 使用 `LoggingSpanExporter`，生产环境需替换为 OTLP Exporter
- Testcontainers 集成测试需要 Docker 环境

## 关键文档

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
| 交付总结 | `docs/phase{1-4}-delivery-summary.md` | 各阶段交付明细 |
