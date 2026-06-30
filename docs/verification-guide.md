# UOK 全阶段操作验证手册

> 本文档覆盖项目全部四个阶段的交付内容、操作步骤与验证方法

---

## 环境准备

```bash
# 前提条件
java -version    # 需要 JDK 17 或 21
mvn -version     # 需要 Maven 3.9+

# 克隆仓库
git clone https://github.com/gavinWithGu/UOK.git
cd UOK

# 确认提交历史完整（15个提交）
git log --oneline
```

预期输出：
```
7956b63 chore: add dependency-reduced-pom.xml to .gitignore
08f46b1 docs: add Phase 4 delivery summary
3ef5f0b feat(phase4): add user-facing documentation
fe2f203 feat(phase4): add GitHub Actions CI/CD pipeline
bda69fb feat(phase4): add TraceValidator utility
05d9d0c feat(phase4): add Filebeat configuration templates
047ee11 feat(phase4): add integration-test module to root POM
14a99e4 feat(phase4): add integration test module with end-to-end tests
8c27e3c feat(phase4): add uok-lambda-starter unit tests and JaCoCo config
7bf5e6b feat(phase4): add uok-agent unit tests
eaa2578 feat(phase4): add config resource files for all modules
a085ad7 feat(phase3): full-scenario instrumentation, Lambda starter, and sample apps
5aaed4d feat(phase2): implement uok-agent core HTTP tracing and Spring Cloud sample
1949983 docs: add Phase 1 delivery summary
883848a feat(phase1): project initialization and uok-common module complete
```

---

## 第一阶段：项目初始化与公共底座

### 1.1 交付内容

| 类别 | 交付物 | 说明 |
|---|---|---|
| 工程骨架 | Maven 多模块 POM | 9个子模块，统一依赖版本管理 |
| 设计文档 | PRD.md / HLD.md / LLD.md | 产品需求、高层设计、低层设计 |
| 核心模块 | uok-common（16个类） | 配置/上下文/采样/灰度/降级/脱敏/指标/工具 |
| 单元测试 | 223 个测试 | 覆盖率 ≥90% 行 / ≥85% 分支 |

### 1.2 uok-common 16个核心类

| 包 | 类 | 职责 |
|---|---|---|
| config | UokConfig | 统一配置模型，Builder模式 |
| config | ConfigLoader | 多源配置加载（系统属性>环境变量>配置文件>默认值） |
| config | SamplerConfig | 采样策略配置 |
| config | GrayConfig | 灰度发布配置（服务/实例/流量多维度） |
| context | TraceContext | W3C Trace Context 数据模型 |
| context | ContextHolder | ThreadLocal 上下文持有 + MDC 同步 |
| context | ReactiveContextHolder | 响应式（WebFlux）上下文持有 |
| constant | LogConstants | 日志字段常量（traceparent 头名等） |
| constant | MetricConstants | Prometheus 指标名称常量 |
| sampler | HeadSampler | 头部采样（按比例 0.0-1.0） |
| sampler | ErrorAlwaysSampler | 错误请求全采样 |
| sampler | TailSampler | 尾部采样（按耗时/错误状态） |
| sampler | DeviceRatioSampler | 按设备比例采样（IoT 场景） |
| gray | GrayController | 灰度控制，支持运行时动态更新 |
| desensitize | DataMasker | 数据脱敏，自动识别敏感字段 |
| degrade | DegradeManager | 降级管控，手动 + 自动双机制，4级降级 |
| utils | TraceIdGenerator | TraceID/SpanID 生成 + W3C 格式化/解析 |

### 1.3 验证步骤

```bash
# Step 1: 编译 uok-common
mvn compile -pl uok-common

# Step 2: 运行 uok-common 全部测试
mvn test -pl uok-common

# Step 3: 验证测试数量（预期 223）
mvn test -pl uok-common 2>&1 | grep "Tests run: 223"

# Step 4: 验证覆盖率（预期 LINE ≥90%, BRANCH ≥85%）
mvn verify -pl uok-common 2>&1 | grep "JaCoCo"
```

### 1.4 关键测试类清单

| 测试类 | 测试数 | 覆盖功能 |
|---|---|---|
| UokConfigTest | 16 | Builder、默认值、环境自动检测 |
| ConfigLoaderTest | 11 | 多源加载优先级、属性键映射 |
| SamplerConfigTest | 12 | 采样配置组合 |
| GrayConfigTest | 28 | 灰度服务/实例/标签/比例 |
| TraceContextTest | 11 | 上下文字段、isRoot判断 |
| ContextHolderTest | 9 | ThreadLocal set/get/clear |
| ReactiveContextHolderTest | 7 | Reactor AtomicReference 上下文 |
| HeadSamplerTest | 6 | 按比例采样 |
| ErrorAlwaysSamplerTest | 5 | 错误全采样 |
| TailSamplerTest | 8 | 尾部采样决策 |
| DeviceRatioSamplerTest | 11 | 设备比例采样 |
| DegradeManagerTest | 14 | 手动/自动降级、4级降级枚举 |
| DataMaskerTest | 18 | 敏感字段自动脱敏 |
| TraceIdGeneratorTest | 32 | ID生成、W3C格式、解析、校验 |
| MetricCollectorTest | 12 | 指标采集 + Prometheus 输出 |
| LogConstantsTest | 6 | 常量正确性 |
| MetricConstantsTest | 8 | 指标名称格式 |

---

## 第二阶段：核心 Agent 与 HTTP 链路能力

### 2.1 交付内容

| 类别 | 交付物 | 说明 |
|---|---|---|
| Agent 入口 | UokAgent | premain/agentmain 双入口 |
| Agent 配置 | AgentConfig | 自动设置 OTel 属性 |
| OTel 集成 | OtelSdkInitializer | SDK 初始化（Tracer/Sampler/Exporter/Propagator） |
| HTTP 埋点 | HttpServletInstrumentation | onHttpRequest/onHttpResponse/onHttpRequestComplete |
| 上下文桥接 | AgentContextHolder | OTel Span ↔ UOK TraceContext 转换 |
| 日志注入 | MdcLogInjector | traceId/spanId/业务字段注入 MDC |
| 样例工程 | sample-springcloud-service | Gateway + Service A + Service B 三服务链路 |

### 2.2 验证步骤

```bash
# Step 1: 编译 uok-agent
mvn compile -pl uok-agent -am

# Step 2: 运行 uok-agent 测试
mvn test -pl uok-agent -am

# Step 3: 验证核心 HTTP 链路测试
mvn test -pl uok-agent -Dtest=HttpServletInstrumentationTest 2>&1 | grep "Tests run:"
# 预期: Tests run: 10

# Step 4: 验证 OTel SDK 初始化
mvn test -pl uok-agent -Dtest=OtelSdkInitializerTest 2>&1 | grep "Tests run:"
# 预期: Tests run: 7

# Step 5: 编译 Spring Cloud 样例
mvn compile -pl samples/sample-springcloud-service -am

# Step 6: 检查 Agent JAR 可构建
mvn package -pl uok-agent -am -DskipTests
ls -la uok-agent/target/uok-agent-1.0.0-SNAPSHOT.jar
```

### 2.3 关键测试类清单

| 测试类 | 测试数 | 覆盖功能 |
|---|---|---|
| HttpServletInstrumentationTest | 10 | 根/子上下文创建、traceparent 提取/注入、MDC 注入/清理 |
| OtelSdkInitializerTest | 7 | OTel SDK 初始化、Tracer/Propagator/Exporter |
| MdcLogInjectorTest | 11 | MDC 注入/清理/业务字段 |
| AgentContextHolderTest | 6 | Span ↔ TraceContext 桥接 |
| AgentConfigTest | 5 | Agent 配置加载 |
| HttpTracingIntegrationTest | 6 | HTTP 链路端到端 |
| UokAgentTest | 8 | premain/agentmain、初始化、禁用 |

---

## 第三阶段：全场景能力扩展

### 3.1 交付内容

| 类别 | 交付物 | 说明 |
|---|---|---|
| Kafka 埋点 | KafkaInstrumentation | onProduce/onConsume，Header 透传 traceId/spanId |
| 异步透传 | AsyncContextPropagator | wrapRunnable/wrapSupplier/wrapExecutor |
| WebFlux | WebFluxInstrumentation | ReactiveContextHolder + wrapFunction |
| 存储埋点 | StorageInstrumentation | MySQL/Redis/DynamoDB 操作 Span |
| 观测管控 | ObservabilityController | 采样+灰度+降级+指标统一入口 |
| Lambda Starter | LambdaTracingInitializer | Lambda 事件追踪（Kinesis/Kafka） |
| Lambda 适配 | KinesisEventAdapter / KafkaEventAdapter | 事件类型识别 + trace 提取 |
| Lambda 上下文 | LambdaContextHolder | Lambda 单事件生命周期上下文 |
| 样例工程 | sample-kafka-app / sample-lambda-function | Kafka 生产消费 / Lambda 函数 |

### 3.2 验证步骤

```bash
# Step 1: 编译所有模块
mvn compile -pl uok-agent,uok-lambda-starter -am

# Step 2: 运行 uok-agent 全部测试
mvn test -pl uok-agent -am 2>&1 | grep "Tests run: 119"

# Step 3: 运行 uok-lambda-starter 全部测试
mvn test -pl uok-lambda-starter -am 2>&1 | grep "Tests run: 70"

# Step 4: 验证 Kafka 埋点
mvn test -pl uok-agent -Dtest=KafkaInstrumentationTest 2>&1 | grep "Tests run:"
# 预期: Tests run: 7

# Step 5: 验证 WebFlux 埋点
mvn test -pl uok-agent -Dtest=WebFluxInstrumentationTest 2>&1 | grep "Tests run:"
# 预期: Tests run: 13

# Step 6: 验证 ObservabilityController（采样+灰度+降级+指标）
mvn test -pl uok-agent -Dtest=ObservabilityControllerTest 2>&1 | grep "Tests run:"
# 预期: Tests run: 29

# Step 7: 验证 Lambda Starter
mvn test -pl uok-lambda-starter 2>&1 | grep "Tests run: 70"

# Step 8: 编译样例工程
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am
```

### 3.3 关键测试类清单

**uok-agent 新增（Phase 3-4）**

| 测试类 | 测试数 | 覆盖功能 |
|---|---|---|
| KafkaInstrumentationTest | 7 | Kafka 生产/消费 trace 透传 |
| AsyncContextPropagatorTest | 10 | 异步线程 trace 传播 |
| WebFluxInstrumentationTest | 13 | 响应式上下文、wrapFunction |
| StorageInstrumentationTest | 7 | MySQL/Redis/DynamoDB Span |
| ObservabilityControllerTest | 29 | 采样决策/灰度/降级/指标/Prometheus |

**uok-lambda-starter 全部**

| 测试类 | 测试数 | 覆盖功能 |
|---|---|---|
| LambdaTracingInitializerTest | 19 | 初始化/事件处理/生命周期/自动初始化 |
| LambdaContextHolderTest | 9 | set/get/clear/null 处理/幂等清理 |
| KinesisEventAdapterTest | 28 | Kinesis 事件识别/Header提取/Data提取/JSON解析 |
| KafkaEventAdapterTest | 14 | Kafka 事件识别/byte[] Header提取/spanId唯一 |

---

## 第四阶段：测试补全 + 性能基准 + 部署交付

### 4.1 交付内容

| 类别 | 交付物 | 说明 |
|---|---|---|
| 单元测试补全 | uok-agent +3类, uok-lambda-starter +4类 | 覆盖率提升至 87.5%/98.3% |
| 集成测试 | EndToEndTracingIntegrationTest | 8 个端到端链路测试 |
| 集成测试 | LambdaTracingIntegrationTest | 7 个 Lambda 集成测试 |
| TraceValidator | TraceValidator + TraceValidatorTest | 链路完整性自动校验 + 17 测试 |
| JMH 基准 | UokPerformanceBenchmark | 11 个性能 benchmark |
| Filebeat 配置 | filebeat.yml + opensearch-index-template.json | OpenSearch 日志采集模板 |
| CI/CD | .github/workflows/ci-cd.yml | GitHub Actions 全流程流水线 |
| 用户文档 | quickstart.md | 5 分钟快速上手 |
| 用户文档 | integration-guide.md | 完整集成手册 |
| 用户文档 | operations-runbook.md | 运维排错手册 |
| 配置资源 | uok.properties × 2, logback-test.xml × 4 | 默认配置和测试日志 |

### 4.2 验证步骤

#### 4.2.1 全量构建验证（最关键的一步）

```bash
# 一键验证所有核心模块的编译、测试、覆盖率
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am
```

预期结果：
```
BUILD SUCCESS
Tests run: 444, Failures: 0, Errors: 0
JaCoCo: uok-common PASS, uok-agent PASS, uok-lambda-starter PASS
```

#### 4.2.2 逐模块测试数验证

```bash
mvn test -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am \
  2>&1 | grep "Tests run:" | grep -v "Time elapsed"
```

预期：
```
uok-common:     223 tests
uok-agent:      119 tests
uok-lambda-starter: 70 tests
integration-test:   32 tests
总计:           444 tests
```

#### 4.2.3 覆盖率验证

```bash
# uok-common: LINE ≥90%, BRANCH ≥85%
mvn verify -pl uok-common 2>&1 | grep -A2 "uok-common.*JaCoCo"

# uok-agent: LINE ≥70%, BRANCH ≥60% (实际 87.5%/76.8%)
mvn verify -pl uok-agent 2>&1 | grep -A2 "uok-agent.*JaCoCo"

# uok-lambda-starter: LINE ≥90%, BRANCH ≥75%
mvn verify -pl uok-lambda-starter 2>&1 | grep -A2 "uok-lambda-starter.*JaCoCo"
```

#### 4.2.4 JMH 性能基准测试

```bash
# 构建 benchmark JAR
mvn -pl test-suite/performance-test -am package -DskipTests

# 运行基准测试（快速验证用 1 warmup × 1 iteration × 1 fork）
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar \
     -wi 1 -i 1 -f 1

# 正式基准测试（3 warmup × 5 iteration × 2 fork）
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar \
     -wi 3 -i 5 -f 2
```

预期基准指标（JDK 17，1 fork 快速验证）：

| Benchmark | 预期范围 |
|---|---|
| traceIdGeneration | ~0.5 μs |
| spanIdGeneration | ~0.4 μs |
| httpRequestWithAgent | ~0.7 μs |
| httpRequestWithoutAgent | ~0.03 μs |
| headSamplingDecision | ~0.5 μs |
| dataMaskingSensitive | ~0.2 μs |
| dataMaskingNonSensitive | ~0.2 μs |
| mdcLogInjection | ~0.8 μs |
| traceParentParsing | ~0.3 μs |
| traceParentFormat | ~0.9 μs |
| httpRequestRootContext | ~0.9 μs |

#### 4.2.5 集成测试验证

```bash
# HTTP 端到端链路追踪
mvn test -pl test-suite/integration-test -Dtest=EndToEndTracingIntegrationTest
# 预期: 8 tests passed

# Lambda 集成测试
mvn test -pl test-suite/integration-test -Dtest=LambdaTracingIntegrationTest
# 预期: 7 tests passed

# TraceValidator 校验
mvn test -pl test-suite/integration-test -Dtest=TraceValidatorTest
# 预期: 17 tests passed
```

#### 4.2.6 文件完整性验证

```bash
# 核心模块代码
echo "=== uok-common 核心类 ==="
find uok-common/src/main/java -name "*.java" | wc -l
# 预期: 17 个文件

echo "=== uok-agent 核心类 ==="
find uok-agent/src/main/java -name "*.java" | wc -l
# 预期: 12+ 个文件

echo "=== uok-lambda-starter 核心类 ==="
find uok-lambda-starter/src/main/java -name "*.java" | wc -l
# 预期: 5+ 个文件

# 部署配置
echo "=== Filebeat 配置 ==="
ls deployment/filebeat-config/
# 预期: filebeat.yml  opensearch-index-template.json  README.md

# CI/CD
echo "=== CI/CD 流水线 ==="
ls .github/workflows/ci-cd.yml
# 预期: 存在

# 用户文档
echo "=== 用户文档 ==="
ls docs/quickstart.md docs/integration-guide.md docs/operations-runbook.md
# 预期: 三个文件均存在

# 交付总结
echo "=== 交付总结 ==="
ls docs/phase1-delivery-summary.md docs/phase2-delivery-summary.md \
   docs/phase3-delivery-summary.md docs/phase4-delivery-summary.md
# 预期: 四个文件均存在

# 样例工程
echo "=== 样例工程 ==="
ls samples/sample-springcloud-service/pom.xml \
   samples/sample-kafka-app/pom.xml \
   samples/sample-lambda-function/pom.xml
# 预期: 三个 POM 均存在
```

#### 4.2.7 样例工程编译验证

```bash
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am
# 预期: 三个样例 BUILD SUCCESS
```

#### 4.2.8 CI/CD 流水线验证

```bash
# 方式1: 在 GitHub 仓库页面
# 打开 https://github.com/gavinWithGu/UOK/actions
# 查看是否有 workflow 定义

# 方式2: 本地验证 YAML 语法
cat .github/workflows/ci-cd.yml | head -10
```

---

## 一键全量验证脚本

将以下内容保存为 `verify-all.sh` 并执行：

```bash
#!/bin/bash
set -e

echo "========================================="
echo "  UOK 全量验证"
echo "========================================="

echo ""
echo ">>> [1/6] 全量构建 + 测试 + 覆盖率"
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am

echo ""
echo ">>> [2/6] 样例工程编译"
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am -q

echo ""
echo ">>> [3/6] JMH 性能基准测试"
mvn -pl test-suite/performance-test -am package -DskipTests -q
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 1 -i 1 -f 1

echo ""
echo ">>> [4/6] 文件完整性检查"
check_file() {
    if [ -f "$1" ]; then echo "  ✅ $1"; else echo "  ❌ $1 MISSING"; fi
}
check_file deployment/filebeat-config/filebeat.yml
check_file deployment/filebeat-config/opensearch-index-template.json
check_file .github/workflows/ci-cd.yml
check_file docs/quickstart.md
check_file docs/integration-guide.md
check_file docs/operations-runbook.md
check_file docs/phase4-delivery-summary.md
check_file test-suite/integration-test/src/test/java/com/bosch/iot/uok/integration/TraceValidator.java

echo ""
echo ">>> [5/6] 测试数量统计"
TOTAL=0
for mod in uok-common uok-agent uok-lambda-starter; do
    COUNT=$(mvn test -pl $mod 2>&1 | grep "Tests run:" | tail -1 | grep -oP '\d+(?=\s*Failures)' || echo "0")
    echo "  $mod: $COUNT tests"
    TOTAL=$((TOTAL + COUNT))
done
INT_COUNT=$(mvn test -pl test-suite/integration-test 2>&1 | grep "Tests run:" | tail -1 | grep -oP '\d+(?=\s*Failures)' || echo "0")
echo "  integration-test: $INT_COUNT tests"
TOTAL=$((TOTAL + INT_COUNT))
echo "  ────────────────────────"
echo "  总计: $TOTAL tests (预期 444)"

echo ""
echo "========================================="
echo "  ✅ 全量验证完成"
echo "========================================="
```

---

## 验收标准对照

| # | 验收标准 | 验证方式 | 预期结果 |
|---|---|---|---|
| 1 | 零侵入 | 检查接入方式 | Agent via `-javaagent`；Lambda 纯 SDK，无 OTel Layer |
| 2 | JDK 17/21 兼容 | `mvn verify` 双 JDK | BUILD SUCCESS |
| 3 | 行覆盖率 ≥90% | JaCoCo 报告 | uok-common 93.4%, uok-lambda-starter 98.3% |
| 4 | OTel 1.32.0 | pom.xml 版本检查 | opentelemetry.version=1.32.0 |
| 5 | Spring Boot 3.2.x | pom.xml 版本检查 | spring-boot.version=3.2.5 |
| 6 | 全量测试通过 | `mvn test` | 444 tests / 0 failures |
| 7 | JMH 基准 | 运行 benchmark | 11 benchmarks, 全链路 <1μs |
| 8 | Filebeat 对接 | 检查配置模板 | filebeat.yml + 索引模板 |
| 9 | CI/CD 流水线 | GitHub Actions | ci-cd.yml 存在且语法正确 |
| 10 | 用户文档 | 文件检查 | quickstart + integration-guide + operations-runbook |
| 11 | W3C Trace Context | 集成测试 | traceparent 格式验证通过 |
