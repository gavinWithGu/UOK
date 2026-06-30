package com.bosch.iot.uok.perf;

import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.desensitize.DataMasker;
import com.bosch.iot.uok.common.sampler.HeadSampler;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * JMH Performance Benchmarks for UOK Agent overhead measurement.
 * <p>
 * Measures the performance impact of UOK instrumentation on:
 * 1. Trace ID generation
 * 2. HTTP request instrumentation (context extraction/injection)
 * 3. Sampling decisions
 * 4. Data masking
 * 5. MDC log injection
 * <p>
 * Run with: java -jar performance-test.jar -wi 3 -i 5 -f 1
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class UokPerformanceBenchmark {

    private UokConfig config;
    private HeadSampler headSampler;
    private DataMasker dataMasker;
    private String existingTraceParent;

    @Setup
    public void setup() {
        config = new UokConfig();
        config.setServiceName("perf-test-service");
        MdcLogInjector.initialize(config);

        headSampler = new HeadSampler(1.0);
        dataMasker = new DataMasker();

        // Pre-generate a valid traceparent for extraction tests
        String traceId = TraceIdGenerator.generateTraceId();
        String spanId = TraceIdGenerator.generateSpanId();
        existingTraceParent = "00-" + traceId + "-" + spanId + "-01";
    }

    // =================== Trace ID Generation ===================

    @Benchmark
    public void traceIdGeneration(Blackhole bh) {
        bh.consume(TraceIdGenerator.generateTraceId());
    }

    @Benchmark
    public void spanIdGeneration(Blackhole bh) {
        bh.consume(TraceIdGenerator.generateSpanId());
    }

    // =================== HTTP Instrumentation ===================

    @Benchmark
    public void httpRequestWithoutAgent(Blackhole bh) {
        // Baseline: no agent, just a simple map lookup
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        bh.consume(headers.get("Content-Type"));
    }

    @Benchmark
    public void httpRequestWithAgent(Blackhole bh) {
        // With UOK: full trace context extraction + MDC injection
        TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
                existingTraceParent, Collections.emptyMap(), "perf-test-service");
        bh.consume(ctx);

        Map<String, String> outgoingHeaders = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(outgoingHeaders);
        bh.consume(outgoingHeaders);

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Benchmark
    public void httpRequestRootContext(Blackhole bh) {
        // UOK: create root context (no incoming traceparent)
        TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "perf-test-service");
        bh.consume(ctx);
        HttpServletInstrumentation.onHttpRequestComplete();
    }

    // =================== Sampling ===================

    @Benchmark
    public void headSamplingDecision(Blackhole bh) {
        bh.consume(headSampler.shouldSample(TraceIdGenerator.generateTraceId()));
    }

    // =================== Data Masking ===================

    @Benchmark
    public void dataMaskingSensitive(Blackhole bh) {
        bh.consume(dataMasker.maskIfSensitive("password", "mySecretPass123"));
    }

    @Benchmark
    public void dataMaskingNonSensitive(Blackhole bh) {
        bh.consume(dataMasker.maskIfSensitive("username", "john"));
    }

    // =================== MDC Injection ===================

    @Benchmark
    public void mdcLogInjection(Blackhole bh) {
        TraceContext context = new TraceContext(
                TraceIdGenerator.generateTraceId(),
                TraceIdGenerator.generateSpanId(),
                "perf-test-service");
        MdcLogInjector.injectTraceContext(context);
        MdcLogInjector.clearTraceContext();
        bh.consume(context);
    }

    // =================== Trace Parent Parsing ===================

    @Benchmark
    public void traceParentParsing(Blackhole bh) {
        bh.consume(TraceIdGenerator.parseTraceParent(existingTraceParent));
    }

    @Benchmark
    public void traceParentFormat(Blackhole bh) {
        String traceId = TraceIdGenerator.generateTraceId();
        String spanId = TraceIdGenerator.generateSpanId();
        bh.consume(TraceIdGenerator.formatTraceParent(traceId, spanId, true));
    }

    /**
     * Main method to run benchmarks.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(UokPerformanceBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
