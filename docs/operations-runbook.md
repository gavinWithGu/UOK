# UOK Operations Runbook

> Monitoring, troubleshooting, and performance tuning for UOK deployments

## 1. Health Check

### 1.1 Verify Agent Is Running

Check application startup logs for UOK initialization:

```
INFO  c.b.i.uok.agent.UokAgent - UOK Agent initialized: service=my-service, env=prod
```

If you don't see this message:
- Verify `-javaagent` flag is in the JVM arguments
- Check `uok.agent.enabled` is not set to `false`
- Check agent JAR file exists and is readable

### 1.2 Verify Trace Context in Logs

Logs should contain `traceId` and `spanId` in MDC:

```
INFO [traceId=abc123 spanId=def456] - Request processed
```

If trace fields are missing:
- Check `uok.trace.enable=true` (default)
- Check `uok.log.enable=true` (default)
- Check degrade status — auto-degrade may have disabled tracing

### 1.3 Verify traceparent Propagation

Use curl to check that responses include the `traceparent` header:

```bash
curl -v http://localhost:8080/api/health 2>&1 | grep traceparent
# Expected: traceparent: 00-0123456789abcdef0123456789abcdef-abcdef0123456789-01
```

## 2. Monitoring

### 2.1 Key Metrics

| Metric | Source | Alert Threshold |
|---|---|---|
| `uok_trace_active` | Prometheus endpoint | 0 = degraded |
| `uok_trace_duration_ms` | OTel export | p99 > 1000ms |
| JVM CPU% | Runtime | > degrade.cpu-threshold |
| Request latency increase | APM | > degrade.latency-increase |

### 2.2 Prometheus Endpoint

If metrics are enabled, UOK exposes metrics at the OTel Prometheus endpoint:

```bash
curl http://localhost:9464/metrics
```

### 2.3 OpenSearch Dashboards

Use OpenSearch Dashboards to visualize traces:

1. Navigate to **Discover**
2. Select index pattern `uok-trace-*`
3. Filter by `traceId` to see a complete chain
4. Filter by `serviceName` + `level:ERROR` for error traces

**Useful queries:**

```
# Find all traces for a specific device
traceId: * AND deviceId: device-iot-001

# Find slow requests (trace chain with multiple hops)
traceId: * AND spanId: * AND parentSpanId: *

# Error traces only
level: ERROR

# Traces for a specific service
serviceName: iot-gateway
```

## 3. Troubleshooting

### 3.1 Missing traceId in Logs

| Symptom | Cause | Fix |
|---|---|---|
| No trace fields at all | Agent not loaded | Check `-javaagent` JVM arg |
| Some requests missing | Sampling rate < 1.0 | Increase `uok.sampler.head-rate` |
| Intermittent missing | Auto-degrade active | Check CPU/latency; lower thresholds |
| WebFlux requests missing | Reactive context not propagated | Use `WebFluxInstrumentation.wrapFunction()` |
| Async threads missing | Context not propagated | Use `AsyncTraceContextPropagator.wrap()` |

### 3.2 Broken Trace Chain

| Symptom | Cause | Fix |
|---|---|---|
| Different traceId across services | `traceparent` not propagated | Verify response headers include `traceparent` |
| parentSpanId not found in chain | Header lost between hops | Check intermediate proxy/gateway strips headers |
| Root entry missing parentSpanId | Expected for root | Normal — root spans have null parentSpanId |

### 3.3 High CPU Overhead

1. **Check sampling rate**: `uok.sampler.head-rate=0.1` for production
2. **Check degrade status**: Auto-degrade should kick in at `cpu-threshold`
3. **Reduce metrics**: `uok.metrics.enable=false` if not needed
4. **Check OTel exporter**: LoggingSpanExporter is default; switch to OTLP for production

### 3.4 Memory Issues

UOK uses ThreadLocal for context storage. In high-concurrency scenarios:

- Ensure `onHttpRequestComplete()` / `onConsumeComplete()` / `onLambdaComplete()` is always called (use `try-finally`)
- Check for thread pool leaks where ThreadLocal isn't cleaned up

### 3.5 Lambda Cold Start Impact

The Lambda Starter initializes OTel SDK on first invocation only (idempotent). Subsequent invocations reuse the SDK:

```
Cold start: ~200ms additional overhead
Warm start: < 1ms per invocation
```

To minimize cold start impact:
- Use provisioned concurrency for latency-sensitive functions
- Set `uok.metrics.enable=false` if metrics are not needed in Lambda

## 4. Performance Tuning

### 4.1 JMH Benchmarks

Run the performance benchmark suite to measure overhead:

```bash
cd UOK
mvn -pl test-suite/performance-test -am package -DskipTests
java -jar test-suite/performance-test/target/performance-test-1.0.0-SNAPSHOT.jar \
     -wi 3 -i 5 -f 2
```

**Expected baseline (JDK 17):**

| Benchmark | Average Time |
|---|---|
| traceIdGeneration | ~0.5 μs |
| spanIdGeneration | ~0.4 μs |
| httpRequestWithAgent | ~0.7 μs |
| httpRequestWithoutAgent | ~0.03 μs |
| headSamplingDecision | ~0.5 μs |
| dataMasking | ~0.2 μs |
| mdcLogInjection | ~0.8 μs |
| traceParentParsing | ~0.3 μs |

**Overhead:** Full HTTP tracing adds approximately **0.7μs per request** — well under 1ms.

### 4.2 Production Configuration Recommendations

| Environment | head-rate | error-always | device-ratio | metrics |
|---|---|---|---|---|
| Development | 1.0 | true | 1.0 | true |
| Staging | 0.5 | true | 1.0 | true |
| Production | 0.1–0.3 | true | 0.5 | false* |

*\* Disable metrics in production unless you have an OTLP/Prometheus collector.*

### 4.3 Degradation Strategy

```
                    CPU > threshold OR Latency > threshold
                                    │
                    ┌───────────────┼───────────────┐
                    ▼                                 ▼
              Auto-degrade                     Manual override
           (REDUCED level)                  (configurable level)
                    │                                 │
                    ▼                                 ▼
          Tracing + Logging                   All / Reduced / Minimal / Disabled
          Metrics disabled                    (via DegradeManager API)
                    │
                    ▼
            Auto-recover when
            conditions normalize
```

## 5. Configuration Reference

### 5.1 System Properties

All UOK properties use the `uok.` prefix and can be set as JVM system properties:

```bash
-Duok.serviceName=my-service
-Duok.env=prod
-Duok.trace.enable=true
-Duok.metrics.enable=false
-Duok.sampler.head-rate=0.3
-Duok.sampler.error-always=true
-Duok.sampler.device-ratio=0.5
-Duok.degrade.cpu-threshold=85
-Duok.degrade.latency-increase=15
```

### 5.2 Environment Variables

Replace dots with underscores and convert to uppercase:

```bash
export UOK_SERVICENAME=my-service
export UOK_ENV=prod
export UOK_TRACE_ENABLE=true
export UOK_SAMPLER_HEAD_RATE=0.3
```

### 5.3 Properties File

Create `uok.properties` on the classpath:

```properties
uok.serviceName=my-service
uok.env=prod
uok.sampler.head-rate=0.3
uok.degrade.cpu-threshold=85
```

**Priority:** System properties > Environment variables > Properties file > Defaults

## 6. Filebeat + OpenSearch Setup

See [deployment/filebeat-config/README.md](../deployment/filebeat-config/README.md) for the complete log shipping configuration.

Quick reference:

```bash
# 1. Deploy Filebeat config
sudo cp deployment/filebeat-config/filebeat.yml /etc/filebeat/filebeat.yml

# 2. Deploy OpenSearch index template
curl -X PUT "https://opensearch:9200/_index_template/uok-trace" \
  -H "Content-Type: application/json" \
  -d @deployment/filebeat-config/opensearch-index-template.json

# 3. Verify
filebeat test config && filebeat test output
```

## 7. Emergency Procedures

### 7.1 Disable UOK Immediately

```bash
# Option 1: System property
-Duok.agent.enabled=false

# Option 2: Manual degrade via JMX/API (if available)
DegradeManager.manualDegrade(DegradeLevel.DISABLED);
```

### 7.2 Restore After Degradation

```java
// Auto-recovery happens automatically when CPU/latency normalize
// For manual recovery:
DegradeManager.recover();
```

### 7.3 TraceValidator — Verify Trace Integrity

Use the built-in `TraceValidator` to validate log files:

```java
import com.bosch.iot.uok.integration.TraceValidator;

TraceValidator validator = TraceValidator.fromLogFile(Path.of("/var/log/apps/my-service/uok-app.log"));
TraceValidator.Result result = validator.validate();

if (!result.isValid()) {
    result.getErrors().forEach(System.err::println);
}
```

Validation checks:
1. Every log entry contains `traceId` and `spanId`
2. All logs in the same trace share the same `traceId`
3. Upstream `spanId` equals downstream `parentSpanId`
4. Root entries have empty `parentSpanId`
5. Error logs are always sampled
