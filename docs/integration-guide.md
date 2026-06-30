# UOK Integration Guide

> Advanced integration, sampling, gray release, and degradation configuration

## 1. HTTP Tracing Integration

### 1.1 Servlet Filter (Spring Boot)

The agent automatically instruments HTTP requests via `HttpServletInstrumentation`. For manual integration:

```java
import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.common.context.TraceContext;

// Incoming request
TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
    request.getHeader("traceparent"),   // W3C traceparent
    headers,                             // Map<String, String> of all headers
    "my-service"                         // service name
);

// Outgoing response — inject traceparent for downstream
Map<String, String> responseHeaders = new HashMap<>();
HttpServletInstrumentation.onHttpResponse(responseHeaders);
// responseHeaders now contains "traceparent: 00-<traceId>-<spanId>-01"

// Request complete — cleanup
HttpServletInstrumentation.onHttpRequestComplete();
```

### 1.2 WebFlux (Reactive)

For Spring WebFlux applications, UOK provides `ReactiveContextHolder` for Reactor context propagation:

```java
import com.bosch.iot.uok.agent.instrumentation.webflux.WebFluxInstrumentation;

// In WebFilter
WebFluxInstrumentation.onWebFluxRequest(exchange);

// Wrap reactive functions for context propagation
Function<T, R> tracedFn = WebFluxInstrumentation.wrapFunction(myFn);

// Cleanup
WebFluxInstrumentation.onWebFluxRequestComplete();
```

## 2. Kafka Integration

### 2.1 Producer (Inject Trace)

```java
import com.bosch.iot.uok.agent.instrumentation.kafka.KafkaInstrumentation;

// Before sending, inject trace into Kafka headers
Map<String, byte[]> headers = new HashMap<>();
KafkaInstrumentation.onProduce(headers);
// headers now contains traceId, spanId as byte[] values

ProducerRecord<String, String> record = new ProducerRecord<>(
    topic, null, null, payload, headers.entrySet()
    .stream()
    .map(e -> new Header(e.getKey(), e.getValue()))
    .collect(Collectors.toList())
);
```

### 2.2 Consumer (Extract Trace)

```java
// On consume, extract trace from Kafka headers
Map<String, byte[]> headers = extractHeaders(consumerRecord);
TraceContext ctx = KafkaInstrumentation.onConsume(headers, "my-consumer-service");

// Process message...

// Cleanup
KafkaInstrumentation.onConsumeComplete();
```

## 3. Async Thread Propagation

UOK provides `AsyncTraceContextPropagator` for tracing across thread boundaries:

```java
import com.bosch.iot.uok.agent.instrumentation.async.AsyncTraceContextPropagator;

// Wrap Runnable for async execution
Runnable tracedRunnable = AsyncTraceContextPropagator.wrap(myRunnable);
executor.submit(tracedRunnable);

// Wrap Callable
Callable<T> tracedCallable = AsyncTraceContextPropagator.wrap(myCallable);
```

## 4. Sampling Configuration

### 4.1 Head-Based Sampling

Controls what percentage of new traces are started:

```bash
# Sample 30% of requests
-Duok.sampler.head-rate=0.3

# Sample everything (development/debugging)
-Duok.sampler.head-rate=1.0
```

### 4.2 Error Always Sampling

Error requests are always sampled regardless of head rate:

```bash
-Duok.sampler.error-always=true   # default
```

### 4.3 Device-Based Sampling

Sample based on device identity ratio (for IoT workloads):

```bash
# Sample 50% of devices
-Duok.sampler.device-ratio=0.5
```

### 4.4 Tail-Based Sampling

Tail sampling makes decisions after the request completes, based on duration or error status:

```java
UokConfig config = new UokConfig.Builder()
    .samplerConfig(new SamplerConfig.Builder()
        .tailSamplingEnabled(true)
        .tailSamplingThresholdMs(500)  // sample if > 500ms
        .build())
    .build();
```

## 5. Gray Release (Canary)

Control which services get the UOK agent via gray release:

### 5.1 Full Gray (All Features)

```bash
-Duok.gray.enable=true
-Duok.gray.ratio=0.2              # 20% of instances
-Duok.gray.serviceList=svc-a,svc-b  # only these services
```

### 5.2 Per-Feature Gray

```bash
-Duok.gray.trace.enable=true
-Duok.gray.trace.ratio=0.5
-Duok.gray.metrics.enable=false
-Duok.gray.log.enable=true
-Duok.gray.log.ratio=1.0
```

## 6. Degradation (Self-Protection)

UOK automatically degrades when the system is under pressure.

### 6.1 Auto-Degradation Levels

| Level | Value | Behavior |
|---|---|---|
| `FULL` | 3 | All features active (tracing + metrics + logging) |
| `REDUCED` | 2 | Tracing + logging only, metrics disabled |
| `MINIMAL` | 1 | Logging only |
| `DISABLED` | 0 | All UOK features disabled |

### 6.2 Configuration

```bash
# Auto-degrade when CPU > 85% or latency increases by 15ms
-Duok.degrade.cpu-threshold=85
-Duok.degrade.latency-increase=15
```

### 6.3 Manual Degradation

```java
DegradeManager dm = UokAgent.getDegradeManager();

// Manual degrade to MINIMAL
dm.manualDegrade(DegradeLevel.MINIMAL);

// Recover
dm.recover();

// Check status
boolean active = dm.isTracingActive();  // true if level >= REDUCED
boolean autoDegraded = dm.isAutoDegraded();
```

## 7. Data Masking

UOK automatically masks sensitive fields in logs:

```java
import com.bosch.iot.uok.common.desensitize.DataMasker;

DataMasker masker = new DataMasker();

// Automatic detection by field name
masker.maskIfSensitive("password", "mySecret123");  // → "******"
masker.maskIfSensitive("username", "john");          // → "john" (not sensitive)

// Custom sensitive field names
DataMasker custom = new DataMasker(Set.of("apiKey", "secret"));
custom.maskIfSensitive("apiKey", "abc-123");  // → "******"
```

Built-in sensitive keywords: `password`, `passwd`, `pwd`, `secret`, `token`, `apikey`, `api_key`, `authorization`, `credential`, `private_key`, `access_key`

## 8. W3C Trace Context

UOK implements the [W3C Trace Context](https://www.w3.org/TR/trace-context/) specification:

### 8.1 traceparent Format

```
traceparent: 00-<traceId>-<spanId>-<flags>
```

| Field | Length | Description |
|---|---|---|
| version | 2 chars | Always `00` |
| traceId | 32 chars | 128-bit hex (lowercase) |
| spanId | 16 chars | 64-bit hex (lowercase) |
| flags | 2 chars | `01` = sampled, `00` = unsampled |

### 8.2 Cross-Service Propagation

```
Gateway ──traceparent──> Service A ──traceparent──> Service B
  traceId=abc               traceId=abc               traceId=abc
  spanId=s1                 spanId=s2                 spanId=s3
                            parentSpanId=s1           parentSpanId=s2
```

All services in the chain share the same `traceId`. Each service generates a unique `spanId` and records the upstream `spanId` as `parentSpanId`.

## 9. OpenTelemetry Integration

UOK uses the OpenTelemetry SDK (1.32.0) programmatically:

```java
// Access the OTel instances (advanced use only)
OpenTelemetry oTel = LambdaTracingInitializer.getOpenTelemetry();
Tracer tracer = LambdaTracingInitializer.getTracer();
```

The OTel SDK is configured with:
- **Propagator**: W3C Trace Context + W3C Baggage
- **Exporter**: LoggingSpanExporter (default), replaceable for production
- **Processor**: BatchSpanProcessor for async export
