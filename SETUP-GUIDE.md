# UOK 项目交付验证指南

> 本文档供另一台电脑上的 Claude Code 或开发者使用，从零开始克隆源码并验证全部四个阶段的交付成果。

---

## 一、环境准备

### 1.1 安装 JDK 17

```bash
# Ubuntu/Debian:
sudo apt update && sudo apt install openjdk-17-jdk -y

# macOS:
brew install openjdk@17

# 手动下载 Temurin JDK 17:
# https://adoptium.net/temurin/releases/?version=17
```

### 1.2 安装 Maven 3.8.7+

```bash
# Ubuntu/Debian:
sudo apt install maven -y

# macOS:
brew install maven

# 手动安装:
# https://maven.apache.org/download.cgi
# 下载后解压并加入 PATH:
export MAVEN_HOME=/path/to/apache-maven-3.9.9
export PATH=$MAVEN_HOME/bin:$PATH
```

### 1.3 安装 Git

```bash
# Ubuntu/Debian:
sudo apt install git -y

# macOS:
brew install git
```

### 1.4 验证环境

```bash
java -version    # 预期: openjdk version "17.x.x"
mvn -version     # 预期: Apache Maven 3.8.7.x
git --version    # 预期: git version 2.x
```

---

## 二、克隆源码

```bash
git clone https://github.com/gavinWithGu/UOK.git
cd UOK
```

### 2.1 确认提交历史（16个提交）

```bash
git log --oneline
```

预期输出：
```
a63f22c docs: add comprehensive verification guide for all 4 phases
7956b63 chore: add dependency-reduced-pom.xml to .gitignore
08f46b1 docs: add Phase 4 delivery summary
3ef5f0b feat(phase4): add user-facing documentation
fe2f203 feat(phase4): add GitHub Actions CI/CD pipeline
bda69fb feat(phase4): add TraceValidator utility for automated trace chain validation
05d9d0c feat(phase4): add Filebeat configuration templates for OpenSearch log shipping
047ee11 feat(phase4): add integration-test module to root POM
14a99e4 feat(phase4): add integration test module with end-to-end tracing tests
8c27e3c feat(phase4): add uok-lambda-starter unit tests and JaCoCo config
7bf5e6b feat(phase4): add uok-agent unit tests - WebFlux, ObservabilityController, UokAgent
eaa2578 feat(phase4): add config resource files for all modules
a085ad7 feat(phase3): full-scenario instrumentation, Lambda starter, and sample apps
5aaed4d feat(phase2): implement uok-agent core HTTP tracing and Spring Cloud sample
1949983 docs: add Phase 1 delivery summary
883848a feat(phase1): project initialization and uok-common module complete
```

### 2.2 确认文件总数

```bash
find . -type f -not -path '*/target/*' -not -path '*/.git/*' | wc -l
# 预期: ~98 个文件
```

---

## 三、一键全量验证（最重要）

```bash
mvn clean verify -pl uok-common,uok-agent,uok-lambda-starter,test-suite/integration-test -am
```

预期结果：
```
Tests run: 444, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> 如果这条命令通过，说明四个阶段的所有核心交付均正确。

---

## 四、分阶段详细验证

### 4.1 第一阶段：项目初始化与公共底座

#### 4.1.1 交付内容

uok-common 模块包含 16 个核心类 + 17 个测试类（223 tests），覆盖：
- 配置加载（UokConfig, ConfigLoader, SamplerConfig, GrayConfig）
- 上下文管理（TraceContext, ContextHolder, ReactiveContextHolder）
- 采样策略（HeadSampler, ErrorAlwaysSampler, TailSampler, DeviceRatioSampler）
- 灰度控制（GrayController）
- 降级管理（DegradeManager）
- 数据脱敏（DataMasker）
- 指标采集（MetricCollector）
- 工具类（TraceIdGenerator）

#### 4.1.2 验证命令

```bash
# 编译
mvn compile -pl uok-common

# 运行全部测试
mvn test -pl uok-common

# 验证测试数量
mvn test -pl uok-common 2>&1 | grep "Tests run: 223"

# 验证覆盖率（LINE ≥90%, BRANCH ≥85%）
mvn verify -pl uok-common 2>&1 | grep -i "jacoco"
```

#### 4.1.3 核心类文件检查

```bash
echo "=== uok-common 核心类检查 ==="
for f in \
  src/main/java/com/bosch/iot/uok/common/config/UokConfig.java \
  src/main/java/com/bosch/iot/uok/common/config/ConfigLoader.java \
  src/main/java/com/bosch/iot/uok/common/config/SamplerConfig.java \
  src/main/java/com/bosch/iot/uok/common/config/GrayConfig.java \
  src/main/java/com/bosch/iot/uok/common/context/TraceContext.java \
  src/main/java/com/bosch/iot/uok/common/context/ContextHolder.java \
  src/main/java/com/bosch/iot/uok/common/context/ReactiveContextHolder.java \
  src/main/java/com/bosch/iot/uok/common/constant/LogConstants.java \
  src/main/java/com/bosch/iot/uok/common/constant/MetricConstants.java \
  src/main/java/com/bosch/iot/uok/common/sampler/Sampler.java \
  src/main/java/com/bosch/iot/uok/common/sampler/HeadSampler.java \
  src/main/java/com/bosch/iot/uok/common/sampler/ErrorAlwaysSampler.java \
  src/main/java/com/bosch/iot/uok/common/sampler/TailSampler.java \
  src/main/java/com/bosch/iot/uok/common/sampler/DeviceRatioSampler.java \
  src/main/java/com/bosch/iot/uok/common/gray/GrayController.java \
  src/main/java/com/bosch/iot/uok/common/desensitize/DataMasker.java \
  src/main/java/com/bosch/iot/uok/common/metrics/MetricCollector.java \
  src/main/java/com/bosch/iot/uok/common/degrade/DegradeManager.java \
  src/main/java/com/bosch/iot/uok/common/utils/TraceIdGenerator.java; do
  if [ -f "uok-common/$f" ]; then echo "  ✅ $f"; else echo "  ❌ $f MISSING"; fi
done
```

---

### 4.2 第二阶段：核心 Agent 与 HTTP 链路能力

#### 4.2.1 交付内容

uok-agent 模块包含 11 个核心类 + 10 个测试类（119 tests），覆盖：
- Agent 入口（UokAgent - premain/agentmain）
- Agent 配置（AgentConfig）
- OTel SDK 集成（OtelSdkInitializer）
- HTTP 埋点（HttpServletInstrumentation）
- 上下文桥接（AgentContextHolder）
- MDC 日志注入（MdcLogInjector）
- Spring Cloud 样例（Gateway + ServiceA + ServiceB）

#### 4.2.2 验证命令

```bash
# 编译 uok-agent
mvn compile -pl uok-agent -am

# 运行全部测试
mvn test -pl uok-agent -am

# 验证测试数量
mvn test -pl uok-agent -am 2>&1 | grep "Tests run: 119"

# 单独验证 HTTP 链路测试
mvn test -pl uok-agent -Dtest=HttpServletInstrumentationTest 2>&1 | grep "Tests run: 10"

# 单独验证 OTel SDK 初始化
mvn test -pl uok-agent -Dtest=OtelSdkInitializerTest 2>&1 | grep "Tests run: 7"

# 构建 Agent JAR
mvn package -pl uok-agent -am -DskipTests
ls -la uok-agent/target/uok-agent-1.0.0-SNAPSHOT.jar
# 预期: JAR 文件存在

# 编译 Spring Cloud 样例
mvn compile -pl samples/sample-springcloud-service -am
```

#### 4.2.3 核心类文件检查

```bash
echo "=== uok-agent 核心类检查 ==="
for f in \
  src/main/java/com/bosch/iot/uok/agent/UokAgent.java \
  src/main/java/com/bosch/iot/uok/agent/config/AgentConfig.java \
  src/main/java/com/bosch/iot/uok/agent/context/AgentContextHolder.java \
  src/main/java/com/bosch/iot/uok/agent/integration/OtelSdkInitializer.java \
  src/main/java/com/bosch/iot/uok/agent/instrumentation/http/HttpServletInstrumentation.java \
  src/main/java/com/bosch/iot/uok/agent/instrumentation/kafka/KafkaInstrumentation.java \
  src/main/java/com/bosch/iot/uok/agent/instrumentation/webflux/WebFluxInstrumentation.java \
  src/main/java/com/bosch/iot/uok/agent/instrumentation/async/AsyncContextPropagator.java \
  src/main/java/com/bosch/iot/uok/agent/instrumentation/storage/StorageInstrumentation.java \
  src/main/java/com/bosch/iot/uok/agent/logging/MdcLogInjector.java \
  src/main/java/com/bosch/iot/uok/agent/metrics/ObservabilityController.java; do
  if [ -f "uok-agent/$f" ]; then echo "  ✅ $f"; else echo "  ❌ $f MISSING"; fi
done
```

---

### 4.3 第三阶段：全场景能力扩展

#### 4.3.1 交付内容

- Kafka/WebFlux/异步/存储埋点（5个类）
- 采样+灰度+降级+指标统一管控（ObservabilityController）
- uok-lambda-starter 模块（4个类 + 4个测试类，70 tests）
- 3个样例工程（sample-kafka-app, sample-lambda-function）

#### 4.3.2 验证命令

```bash
# 编译 uok-lambda-starter
mvn compile -pl uok-lambda-starter -am

# 运行全部测试
mvn test -pl uok-lambda-starter -am

# 验证测试数量
mvn test -pl uok-lambda-starter -am 2>&1 | grep "Tests run: 70"

# 逐项验证 Kafka 埋点
mvn test -pl uok-agent -Dtest=KafkaInstrumentationTest 2>&1 | grep "Tests run: 7"

# 逐项验证 WebFlux 埋点
mvn test -pl uok-agent -Dtest=WebFluxInstrumentationTest 2>&1 | grep "Tests run: 13"

# 逐项验证 ObservabilityController
mvn test -pl uok-agent -Dtest=ObservabilityControllerTest 2>&1 | grep "Tests run: 29"

# 逐项验证 Lambda Starter
mvn test -pl uok-lambda-starter -Dtest=LambdaTracingInitializerTest 2>&1 | grep "Tests run: 19"
mvn test -pl uok-lambda-starter -Dtest=KinesisEventAdapterTest 2>&1 | grep "Tests run: 28"
mvn test -pl uok-lambda-starter -Dtest=KafkaEventAdapterTest 2>&1 | grep "Tests run: 14"
mvn test -pl uok-lambda-starter -Dtest=LambdaContextHolderTest 2>&1 | grep "Tests run: 9"

# 编译全部样例工程
mvn compile -pl samples/sample-springcloud-service,samples/sample-kafka-app,samples/sample-lambda-function -am
```

#### 4.3.3 Lambda Starter 核心类检查

```bash
echo "=== uok-lambda-starter 核心类检查 ==="
for f in \
  src/main/java/com/bosch/iot/uok/lambda/LambdaTracingInitializer.java \
  src/main/java/com/bosch/iot/uok/lambda/adapter/KinesisEventAdapter.java \
  src/main/java/com/bosch/iot/uok/lambda/adapter/KafkaEventAdapter.java \
  src/main/java/com/bosch/iot/uok/lambda/context/LambdaContextHolder.java; do
  if [ -f "uok-lambda-starter/$f" ]; then echo "  ✅ $f"; else echo "  ❌ $f MISSING"; fi
done
```

---

### 4.4 第四阶段：测试补全 + 性能基准 + 部署交付

#### 4.4.1 交付内容

- 集成测试模块（32 tests: EndToEndTracing 8 + LambdaTracing 7 + TraceValidator 17）
- JMH 性能基准测试（11 benchmarks）
- Filebeat 配置模板（filebeat.yml + opensearch-index-template.json）
- TraceValidator 链路校验工具
- GitHub Actions CI/CD 流水线
- 用户文档（quickstart + integration-guide + operations-runbook）
- 配置资源文件（uok.properties × 2, logback-test.xml × 4）

#### 4.4.2 验证命令

```bash
# === 集成测试 ===

# 运行集成测试
mvn test -pl test-suite/integration-test -am

# 逐项验证
mvn test -pl test-suite/integration-test -Dtest=EndToEndTracingIntegrationTest 2>&1 | grep "Tests run: 8"
mvn test -pl test-suite/integration-test -Dtest=LambdaTracingIntegrationTest 2>&1 | grep "Tests run: 7"
mvn test -pl test-suite/integration-test -Dtest=TraceValidatorTest 2>&1 | grep "Tests run: 17"

# === JMH 性能基准测试 ===

# 构建 benchmark JAR
mvn -pl test-suite/performance-test -am package -DskipTests

# 快速运行（1 warmup × 1 iteration × 1 fork，约 30 秒）
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 1 -i 1 -f 1

# 正式运行（3 warmup × 5 iteration × 2 fork，约 5 分钟）
# java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar -wi 3 -i 5 -f 2

# 预期基准指标:
#   httpRequestWithAgent:     ~0.7 μs
#   httpRequestWithoutAgent:  ~0.03 μs
#   traceIdGeneration:        ~0.5 μs
#   spanIdGeneration:         ~0.4 μs
#   headSamplingDecision:     ~0.5 μs
#   dataMasking:              ~0.2 μs
#   mdcLogInjection:          ~0.8 μs
#   traceParentParsing:       ~0.3 μs
#   traceParentFormat:        ~0.9 μs
#   httpRequestRootContext:    ~0.9 μs

# === 覆盖率验证 ===

# 查看各模块覆盖率（需先运行 mvn verify）
echo "uok-common 覆盖率:"
cat uok-common/target/site/jacoco/index.html 2>/dev/null | grep -oP '\d+%' | head -2 || echo "  运行 mvn verify -pl uok-common 后查看"

echo "uok-agent 覆盖率:"
cat uok-agent/target/site/jacoco/index.html 2>/dev/null | grep -oP '\d+%' | head -2 || echo "  运行 mvn verify -pl uok-agent 后查看"

echo "uok-lambda-starter 覆盖率:"
cat uok-lambda-starter/target/site/jacoco/index.html 2>/dev/null | grep -oP '\d+%' | head -2 || echo "  运行 mvn verify -pl uok-lambda-starter 后查看"
```

#### 4.4.3 文件完整性检查

```bash
echo "=== 第四阶段交付文件检查 ==="

# Filebeat 配置
check_file() {
    if [ -f "$1" ]; then echo "  ✅ $1"; else echo "  ❌ $1 MISSING"; fi
}

check_file "deployment/filebeat-config/filebeat.yml"
check_file "deployment/filebeat-config/opensearch-index-template.json"
check_file "deployment/filebeat-config/README.md"

# CI/CD
check_file ".github/workflows/ci-cd.yml"

# TraceValidator
check_file "test-suite/integration-test/src/test/java/com/bosch/iot/uok/integration/TraceValidator.java"
check_file "test-suite/integration-test/src/test/java/com/bosch/iot/uok/integration/TraceValidatorTest.java"

# 用户文档
check_file "docs/quickstart.md"
check_file "docs/integration-guide.md"
check_file "docs/operations-runbook.md"
check_file "docs/verification-guide.md"

# 交付总结
check_file "docs/phase1-delivery-summary.md"
check_file "docs/phase2-delivery-summary.md"
check_file "docs/phase3-delivery-summary.md"
check_file "docs/phase4-delivery-summary.md"

# 设计文档
check_file "docs/PRD.md"
check_file "docs/HLD.md"
check_file "docs/LLD.md"

# 配置资源
check_file "uok-agent/src/main/resources/uok.properties"
check_file "uok-lambda-starter/src/main/resources/uok.properties"
check_file "uok-common/src/test/resources/logback-test.xml"
check_file "uok-agent/src/test/resources/logback-test.xml"
check_file "uok-lambda-starter/src/test/resources/logback-test.xml"
check_file "test-suite/integration-test/src/test/resources/logback-test.xml"
```

---

## 五、使用 Claude Code 验证

在新电脑上安装 Claude Code 后：

```bash
cd UOK
claude
```

在 Claude Code 中输入：

```
请按照 SETUP-GUIDE.md 执行全量验证，从环境检查开始，逐阶段验证所有交付物，输出验证报告
```

Claude Code 会自动：
1. 检查 JDK/Maven 环境
2. 执行 `mvn clean verify`
3. 逐阶段运行测试
4. 检查文件完整性
5. 运行 JMH 基准测试
6. 输出完整验证报告

---

## 六、预期验证结果汇总

### 测试统计

| 模块 | 测试数 | 通过率 |
|---|---|---|
| uok-common | 223 | 100% |
| uok-agent | 119 | 100% |
| uok-lambda-starter | 70 | 100% |
| integration-test | 32 | 100% |
| **总计** | **444** | **100%** |

### 覆盖率

| 模块 | 行覆盖率 | 分支覆盖率 | JaCoCo 阈值 |
|---|---|---|---|
| uok-common | ≥93% | ≥86% | LINE ≥90%, BRANCH ≥85% |
| uok-agent | ≥87% | ≥76% | LINE ≥70%, BRANCH ≥60% |
| uok-lambda-starter | ≥98% | ≥86% | LINE ≥90%, BRANCH ≥75% |

### JMH 基准

| Benchmark | 预期耗时 |
|---|---|
| httpRequestWithAgent | ~0.7 μs |
| httpRequestWithoutAgent | ~0.03 μs |
| traceIdGeneration | ~0.5 μs |
| spanIdGeneration | ~0.4 μs |

### 构建结果

```
mvn clean verify → BUILD SUCCESS
mvn compile samples → BUILD SUCCESS (3 个样例)
JMH benchmarks → 11/11 passed
```

---

## 七、常见问题排查

| 问题 | 原因 | 解决 |
|---|---|---|
| `java: 错误: 不支持的发行版本 17` | JDK 版本不对 | 安装 JDK 17 并设置 `JAVA_HOME` |
| `Could not find artifact` | 模块间依赖未安装 | 先执行 `mvn install -pl uok-common -DskipTests` |
| JaCoCo check 失败 | 覆盖率未达标 | 确认未修改源码，重新 `mvn clean verify` |
| JMH 运行报 ClassNotFoundException | 未先 package | 先 `mvn -pl test-suite/performance-test -am package -DskipTests` |
| `SLF4J: No SLF4J providers` | JMH 运行时正常警告 | 不影响结果，可忽略 |

---

## 八、验收标准对照

| # | 验收标准 | 验证方式 | 预期结果 |
|---|---|---|---|
| 1 | 零侵入 | 检查接入方式 | Agent via `-javaagent`；Lambda 纯 SDK |
| 2 | JDK 17/21 兼容 | `mvn verify` | BUILD SUCCESS |
| 3 | 行覆盖率 ≥90% | JaCoCo 报告 | uok-common ≥93%, uok-lambda ≥98% |
| 4 | OTel 1.32.0 | `grep opentelemetry pom.xml` | 1.32.0 |
| 5 | Spring Boot 3.2.x | `grep spring-boot pom.xml` | 3.2.5 |
| 6 | 全量测试通过 | `mvn test` | 444 tests / 0 failures |
| 7 | JMH 基准 | 运行 benchmark | 11 benchmarks, 全链路 <1μs |
| 8 | Filebeat 对接 | 检查配置文件 | filebeat.yml + 索引模板存在 |
| 9 | CI/CD 流水线 | 检查 ci-cd.yml | 文件存在且语法正确 |
| 10 | 用户文档 | 检查 3 份 .md | quickstart + integration + operations |
| 11 | W3C Trace Context | 集成测试 | EndToEndTracingIntegrationTest 通过 |
