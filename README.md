# Unified Observability Kit (UOK)

> 零侵入 Java 可观测性工具包 — 分布式链路追踪 · 指标采集 · 日志关联

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen)](https://github.com/gavinWithGu/UOK)
[![Tests](https://img.shields.io/badge/tests-444%20passed-blue)](https://github.com/gavinWithGu/UOK)
[![Coverage](https://img.shields.io/badge/coverage-93%25%2B-green)](https://github.com/gavinWithGu/UOK)
[![JDK](https://img.shields.io/badge/JDK-17%20%7C%2021-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)

---

## 项目简介

UOK 是面向 IoT 平台的统一可观测性工具包，以**零业务侵入**方式为 Java 应用提供：

- 🔗 **分布式链路追踪** — W3C Trace Context 标准，跨 HTTP / Kafka / Lambda / WebFlux / 异步线程全链路传播
- 📊 **指标采集** — QPS / 错误率 / 分位耗时，Prometheus 格式输出
- 📝 **日志关联** — traceId / spanId 自动注入 MDC，一条日志即可定位全链路
- 🛡️ **自我保护** — 多级降级 + 灰度控制 + 数据脱敏

---

## 主要功能

### 链路追踪

| 场景 | 实现类 | 说明 |
|---|---|---|
| HTTP 请求 | `HttpServletInstrumentation` | Servlet 入口自动创建/提取 trace 上下文，出口注入 `traceparent` |
| Kafka 消息 | `KafkaInstrumentation` | 生产端注入 trace 到 Headers，消费端自动提取 |
| Lambda 事件 | `LambdaTracingInitializer` | 支持 Kinesis / Kafka(MSK) 事件，纯 SDK 无需 OTel Layer |
| WebFlux | `WebFluxInstrumentation` | Reactor 上下文传播，`wrapFunction` 包装响应式函数 |
| 异步线程 | `AsyncContextPropagator` | `wrapRunnable` / `wrapSupplier` / `wrapExecutor` 跨线程透传 |
| 数据库操作 | `StorageInstrumentation` | MySQL / Redis / DynamoDB 操作自动生成 Span |

### 采样策略

| 策略 | 类 | 说明 |
|---|---|---|
| 头部采样 | `HeadSampler` | 按比例（0.0–1.0）控制新 trace 创建 |
| 错误全采 | `ErrorAlwaysSampler` | 错误请求 100% 保留 |
| 尾部采样 | `TailSampler` | 请求完成后按耗时/错误决定是否保留 |
| 设备采样 | `DeviceRatioSampler` | 按设备 ID 哈希比例采样（IoT 场景） |

### 灰度与降级

| 能力 | 实现 | 说明 |
|---|---|---|
| 灰度控制 | `GrayController` | 按服务名 / 实例 IP / 标签 / 流量比例控制接入 |
| 自动降级 | `DegradeManager` | CPU / 延迟超阈值自动降级（4 级：FULL → REDUCED → MINIMAL → DISABLED） |
| 手动降级 | `DegradeManager` | API 调用立即切换降级级别，恢复时自动回升 |
| 数据脱敏 | `DataMasker` | 自动识别 password/token/secret 等字段，脱敏为 `******` |

---

## 技术栈

| 组件 | 版本 | 用途 |
|---|---|---|
| Java | 17 / 21 | 运行时（双版本兼容） |
| OpenTelemetry SDK | 1.32.0 | 链路追踪基础设施 |
| Spring Boot | 3.2.5 | 样例工程框架 |
| JUnit 5 | 5.10.2 | 单元测试 |
| Mockito | 5.11.0 | Mock 测试 |
| Testcontainers | 1.19.7 | 集成测试 |
| JMH | 1.37 | 性能基准测试 |
| JaCoCo | 0.8.11 | 覆盖率统计 |
| Maven | 3.8.7+ | 构建工具 |

---

## 快速开始

### 前提条件

- JDK 17 或 21
- Maven 3.8.7+

### 方式一：Java Agent（Servlet / Spring Boot 应用）

**1. 构建 Agent JAR**

```bash
git clone https://github.com/gavinWithGu/UOK.git
cd UOK
mvn -pl uok-agent -am package -DskipTests
```

**2. 附加到你的应用**

```bash
java -javaagent:uok-agent/target/uok-agent-1.0.0-SNAPSHOT.jar \
     -Duok.serviceName=my-service \
     -Duok.env=prod \
     -jar your-app.jar
```

**3. 验证 — 日志中自动出现追踪字段**

```
2026-06-29 10:00:00 [http-exec-1] INFO  c.e.MyController [traceId=abc123 spanId=def456] - Request processed
```

### 方式二：Lambda Starter（AWS Lambda 函数）

**1. 添加依赖**

```xml
<dependency>
    <groupId>com.bosch.iot.uok</groupId>
    <artifactId>uok-lambda-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**2. 三行代码接入**

```java
import com.bosch.iot.uok.lambda.LambdaTracingInitializer;
import com.bosch.iot.uok.common.context.TraceContext;

public class MyHandler implements RequestHandler<Object, String> {
    @Override
    public String handleRequest(Object event, Context context) {
        LambdaTracingInitializer.initialize();
        TraceContext ctx = LambdaTracingInitializer.onLambdaEvent(event);
        try {
            return doWork(event);
        } finally {
            LambdaTracingInitializer.onLambdaComplete();
        }
    }
}
```

> 无需 AWS OTel Lambda Layer，纯 SDK 实现。

---

## 配置说明

### 配置优先级

系统属性 > 环境变量 > `uok.properties` 文件 > 默认值

### 核心配置项

| 属性 | 默认值 | 说明 |
|---|---|---|
| `uok.serviceName` | `unknown-service` | 服务名称 |
| `uok.env` | `dev` | 环境（dev/test/prod，自动检测） |
| `uok.trace.enable` | `true` | 是否启用链路追踪 |
| `uok.metrics.enable` | `true` | 是否启用指标采集 |
| `uok.log.enable` | `true` | 是否启用 MDC 日志注入 |
| `uok.sampler.head-rate` | `1.0` | 头部采样比例（0.0–1.0） |
| `uok.sampler.error-always` | `true` | 错误请求全采样 |
| `uok.sampler.device-ratio` | `1.0` | 设备采样比例 |
| `uok.degrade.cpu-threshold` | `80` | CPU 超阈值自动降级（%） |
| `uok.degrade.latency-increase` | `10` | 延迟增加超阈值自动降级（ms） |

### 示例：生产环境配置

```bash
java -javaagent:uok-agent.jar \
     -Duok.serviceName=iot-gateway \
     -Duok.env=prod \
     -Duok.sampler.head-rate=0.3 \
     -Duok.sampler.error-always=true \
     -Duok.degrade.cpu-threshold=85 \
     -jar your-app.jar
```

---

## W3C Trace Context

UOK 严格遵循 [W3C Trace Context](https://www.w3.org/TR/trace-context/) 规范：

```
traceparent: 00-0123456789abcdef0123456789abcdef-abcdef0123456789-01
              ↑  ↑──────────────────────────────↑ ↑──────────────↑ ↑
           version        traceId (128-bit)       spanId (64-bit)  flags
```

跨服务传播链路：

```
Gateway ──traceparent──> Service A ──traceparent──> Service B
  traceId=abc              traceId=abc              traceId=abc
  spanId=s1                spanId=s2                spanId=s3
                           parentSpanId=s1          parentSpanId=s2
```

---

## 模块结构

```
UOK/
├── uok-common/              # 公共核心（配置/上下文/采样/灰度/降级/脱敏/指标）
├── uok-agent/               # Java Agent（HTTP/Kafka/WebFlux/DB 埋点）
├── uok-lambda-starter/      # Lambda Starter SDK（Kinesis/Kafka 事件追踪）
├── samples/                 # 样例工程
│   ├── sample-springcloud-service/   # Gateway + ServiceA + ServiceB
│   ├── sample-kafka-app/             # Kafka 生产者 + 消费者
│   └── sample-lambda-function/       # Lambda 函数
├── test-suite/              # 测试套件
│   ├── integration-test/    # 端到端集成测试
│   └── performance-test/    # JMH 性能基准
├── deployment/              # 部署配置
│   └── filebeat-config/     # Filebeat + OpenSearch 模板
├── docs/                    # 设计文档 + 用户文档
└── .github/workflows/       # CI/CD 流水线
```

---

## 构建 & 测试

```bash
# 全量构建 + 测试 + 覆盖率
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am

# 只运行测试
mvn test -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am

# JMH 性能基准
mvn -pl test-suite/performance-test -am package -DskipTests
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 1 -i 1 -f 1

# 样例工程编译
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am
```

### 测试与覆盖率

| 模块 | 测试数 | 行覆盖率 | 分支覆盖率 |
|---|---|---|---|
| uok-common | 223 | 93.4% | 86.7% |
| uok-agent | 119 | 87.5% | 76.8% |
| uok-lambda-starter | 70 | 98.3% | 86.4% |
| integration-test | 32 | — | — |
| **总计** | **444** | — | — |

### 性能基准

| 指标 | 耗时 |
|---|---|
| HTTP 全链路（含 Agent） | ~0.7 μs |
| HTTP 基线（无 Agent） | ~0.03 μs |
| traceId 生成 | ~0.5 μs |
| 采样决策 | ~0.5 μs |
| 数据脱敏 | ~0.2 μs |

---

## 日志采集（Filebeat → OpenSearch）

```bash
# 1. 部署 Filebeat 配置
sudo cp deployment/filebeat-config/filebeat.yml /etc/filebeat/filebeat.yml

# 2. 部署 OpenSearch 索引模板
curl -X PUT "https://opensearch:9200/_index_template/uok-trace" \
  -H "Content-Type: application/json" \
  -d @deployment/filebeat-config/opensearch-index-template.json

# 3. 验证
filebeat test config && filebeat test output
```

日志按 `uok-trace-<serviceName>-<日期>` 索引拆分，支持 OpenSearch 文档级权限隔离。

详见 [deployment/filebeat-config/README.md](deployment/filebeat-config/README.md)。

---

## 文档

| 文档 | 说明 |
|---|---|
| [快速上手](docs/quickstart.md) | 5 分钟接入指南 |
| [集成手册](docs/integration-guide.md) | HTTP/Kafka/采样/灰度/降级/脱敏详细配置 |
| [运维手册](docs/operations-runbook.md) | 监控/排错/性能调优/紧急处理 |
| [验证指南](SETUP-GUIDE.md) | 从零开始的全量验证步骤 |
| [项目全景](PROJECT-GUIDE.md) | 单文件项目全貌（供 Agent 阅读） |
| [设计文档](docs/PRD.md) | PRD / HLD / LLD |

### 📖 新人推荐阅读顺序

| 顺序 | 文档 | 目的 |
|---|---|---|
| 1 | [README.md](README.md) | 建立全局印象：项目是什么、能做什么、技术栈与模块结构 |
| 2 | [Unified Observability Kit(UOK)_Design.md](Unified%20Observability%20Kit(UOK)_Design.md) | 理解设计思路：PRD+HLD+LLD 合并版，需求来源、分层架构、模块设计一文档读完 |
| 3 | [PROJECT-GUIDE.md](PROJECT-GUIDE.md) | 项目全景：四阶段开发过程、完整文件目录、编码约定、验收标准达成 |
| 4 | [development-phases.md](docs/development-phases.md) | 开发执行记录：每阶段的目标、交付、设计决策、覆盖率变化 |
| 5 | [SETUP-GUIDE.md](SETUP-GUIDE.md) | **动手验证（核心）**：从零克隆源码、环境准备、一键构建、分阶段测试、覆盖率与 JMH 基准 |
| 6 | [quickstart.md](docs/quickstart.md) | 接入使用：Agent 一行启动 / Lambda 三行代码 |
| 7 | [integration-guide.md](docs/integration-guide.md) | 深入配置：采样/灰度/降级/脱敏完整参数 |
| 8 | [operations-runbook.md](docs/operations-runbook.md) | 运维保障：监控指标、排错表、性能调优、紧急处理 |

> **快速路径**：如果只想验证项目能否正常构建，直接看 [SETUP-GUIDE.md](SETUP-GUIDE.md)。
>
> **深入路径**：想了解某个阶段的交付细节，阅读 [docs/phase{1-4}-delivery-summary.md](docs/phase1-delivery-summary.md)。

---

## License

Apache License 2.0
