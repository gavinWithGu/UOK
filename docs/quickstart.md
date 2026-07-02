# UOK Quick Start Guide

> Unified Observability Kit — Zero-intrusion Java observability toolkit for IoT platforms

## 1. Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 or 21 (OpenJDK / Temurin) |
| Maven | 3.8.7+ |
| Application | Spring Boot 3.2.x (recommended) or any Java servlet app |

## 2. Option A: Java Agent (Servlet / Spring Boot Applications)

### 2.1 Build the Agent

```bash
git clone https://github.com/gavinWithGu/UOK.git
cd UOK
mvn -pl uok-agent -am package -DskipTests
```

### 2.2 Attach to Your Application

Add the `-javaagent` flag to your JVM startup:

```bash
java -javaagent:uok-agent/target/uok-agent-1.0.0-SNAPSHOT.jar \
     -Duok.serviceName=my-service \
     -Duok.env=prod \
     -jar your-app.jar
```

### 2.3 That's It — Zero Code Changes Required

The agent automatically:
- Creates trace context on every HTTP request
- Propagates `traceparent` header (W3C Trace Context)
- Injects `traceId`, `spanId`, `serviceName` into SLF4J MDC
- Supports Kafka produce/consume, WebFlux, async threads, and scheduled tasks

### 2.4 Verify It's Working

Check your logs for trace fields:

```
2026-06-29 10:00:00 [http-exec-1] INFO  c.e.MyController [traceId=abc123 spanId=def456] - Request processed
```

## 3. Option B: Lambda Starter (AWS Lambda Functions)

### 3.1 Add Dependency

```xml
<dependency>
    <groupId>com.bosch.iot.uok</groupId>
    <artifactId>uok-lambda-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 3.2 Instrument Your Handler

```java
import com.bosch.iot.uok.lambda.LambdaTracingInitializer;
import com.bosch.iot.uok.common.context.TraceContext;

public class MyLambdaHandler implements RequestHandler<Object, String> {

    @Override
    public String handleRequest(Object event, Context context) {
        // Initialize tracing (idempotent, safe to call each invocation)
        LambdaTracingInitializer.initialize();

        // Extract trace from event (Kinesis/Kafka) or create root
        TraceContext traceCtx = LambdaTracingInitializer.onLambdaEvent(event);

        try {
            // Your business logic — MDC now has traceId/spanId
            return doWork(event);
        } finally {
            // Always clean up
            LambdaTracingInitializer.onLambdaComplete();
        }
    }
}
```

### 3.3 No AWS OTel Layer Required

UOK Lambda Starter is a pure SDK — no Lambda Layer, no extension, no sidecar.

## 4. Configuration

### 4.1 Priority Order

System properties > Environment variables > `uok.properties` file > Defaults

### 4.2 Key Properties

| Property | Default | Description |
|---|---|---|
| `uok.serviceName` | `unknown-service` | Your service name |
| `uok.env` | `dev` | Environment (dev/test/prod) |
| `uok.trace.enable` | `true` | Enable distributed tracing |
| `uok.metrics.enable` | `true` | Enable metrics collection |
| `uok.log.enable` | `true` | Enable MDC log injection |
| `uok.sampler.head-rate` | `1.0` | Head-based sampling rate (0.0–1.0) |
| `uok.sampler.error-always` | `true` | Always sample error requests |
| `uok.sampler.device-ratio` | `1.0` | Device-based sampling ratio |
| `uok.degrade.cpu-threshold` | `80` | CPU% threshold for auto-degrade |
| `uok.degrade.latency-increase` | `10` | Latency increase (ms) threshold |

### 4.3 Example: Production Configuration

```bash
java -javaagent:uok-agent.jar \
     -Duok.serviceName=iot-gateway \
     -Duok.env=prod \
     -Duok.sampler.head-rate=0.3 \
     -Duok.sampler.error-always=true \
     -Duok.degrade.cpu-threshold=85 \
     -jar your-app.jar
```

## 5. Log Format

UOK outputs structured JSON logs compatible with Filebeat → OpenSearch:

```json
{
  "timestamp": "2026-06-29T10:00:00.000Z",
  "level": "INFO",
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "abcdef0123456789",
  "parentSpanId": "1234567890abcdef",
  "serviceName": "iot-gateway",
  "bizDomain": "iot-home",
  "teamName": "backend-team",
  "env": "prod",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.example.MyController",
  "message": "Request processed"
}
```

## 6. What's Next?

- [Integration Guide](integration-guide.md) — Advanced configuration, sampling, gray release, degradation
- [Operations Runbook](operations-runbook.md) — Monitoring, troubleshooting, performance tuning
- [Filebeat Configuration](../deployment/filebeat-config/README.md) — OpenSearch log shipping setup
