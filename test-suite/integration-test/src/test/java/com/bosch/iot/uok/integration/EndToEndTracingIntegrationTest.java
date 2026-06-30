package com.bosch.iot.uok.integration;

import com.bosch.iot.uok.agent.context.AgentContextHolder;
import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.agent.instrumentation.kafka.KafkaInstrumentation;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test for HTTP trace chain propagation.
 * Simulates Gateway -> Service A -> Service B call chain
 * and verifies trace context consistency across hops.
 */
class EndToEndTracingIntegrationTest {

    @BeforeEach
    void setUp() {
        ContextHolder.remove();
        MDC.clear();
        MdcLogInjector.initialize(new UokConfig());
    }

    @AfterEach
    void tearDown() {
        ContextHolder.remove();
        MDC.clear();
    }

    @Test
    @DisplayName("E2E: Should maintain consistent traceId across HTTP call chain")
    void shouldMaintainConsistentTraceIdAcrossChain() {
        // === Gateway (entry point) ===
        TraceContext gatewayCtx = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "gateway");

        String traceId = gatewayCtx.getTraceId();
        String gatewaySpanId = gatewayCtx.getSpanId();

        assertThat(traceId).isNotNull();
        assertThat(gatewaySpanId).isNotNull();
        assertThat(gatewayCtx.isRoot()).isTrue();

        // Gateway injects traceparent into outgoing headers
        Map<String, String> outgoingHeaders = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(outgoingHeaders);
        String traceparent = outgoingHeaders.get(LogConstants.TRACE_PARENT_HEADER);
        assertThat(traceparent).isNotNull();
        assertThat(traceparent).startsWith("00-");

        HttpServletInstrumentation.onHttpRequestComplete();

        // === Service A (receives from Gateway) ===
        TraceContext serviceACtx = HttpServletInstrumentation.onHttpRequest(
                traceparent, Collections.emptyMap(), "service-a");

        assertThat(serviceACtx.getTraceId()).isEqualTo(traceId);
        assertThat(serviceACtx.getParentSpanId()).isEqualTo(gatewaySpanId);
        assertThat(serviceACtx.isRoot()).isFalse();

        String serviceASpanId = serviceACtx.getSpanId();

        // Service A injects traceparent for outgoing call
        Map<String, String> serviceAOutgoing = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(serviceAOutgoing);
        String serviceATraceparent = serviceAOutgoing.get(LogConstants.TRACE_PARENT_HEADER);

        HttpServletInstrumentation.onHttpRequestComplete();

        // === Service B (receives from Service A) ===
        TraceContext serviceBCtx = HttpServletInstrumentation.onHttpRequest(
                serviceATraceparent, Collections.emptyMap(), "service-b");

        assertThat(serviceBCtx.getTraceId()).isEqualTo(traceId);
        assertThat(serviceBCtx.getParentSpanId()).isEqualTo(serviceASpanId);
        assertThat(serviceBCtx.isRoot()).isFalse();

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("E2E: Should propagate business fields through HTTP chain")
    void shouldPropagateBusinessFieldsThroughChain() {
        Map<String, String> headers = new HashMap<>();
        headers.put("deviceId", "device-iot-001");
        headers.put("userId", "user-42");

        TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
                null, headers, "gateway");

        assertThat(MDC.get("deviceId")).isEqualTo("device-iot-001");
        assertThat(MDC.get("userId")).isEqualTo("user-42");

        HttpServletInstrumentation.onHttpRequestComplete();
        assertThat(MDC.get("deviceId")).isNull();
    }

    @Test
    @DisplayName("E2E: Should maintain traceId across Kafka produce-consume cycle")
    void shouldMaintainTraceIdAcrossKafkaCycle() {
        // Producer creates context
        TraceContext producerCtx = AgentContextHolder.createRootContext("kafka-producer");
        ContextHolder.set(producerCtx); // Set in ThreadLocal for onProduce to find
        String traceId = producerCtx.getTraceId();
        String producerSpanId = producerCtx.getSpanId();

        // Producer injects into Kafka headers (byte[] values)
        Map<String, byte[]> kafkaHeaders = new HashMap<>();
        KafkaInstrumentation.onProduce(kafkaHeaders);

        assertThat(new String(kafkaHeaders.get("traceId"), StandardCharsets.UTF_8)).isEqualTo(traceId);
        assertThat(new String(kafkaHeaders.get("spanId"), StandardCharsets.UTF_8)).isEqualTo(producerSpanId);

        // Consumer extracts from Kafka headers
        TraceContext consumerCtx = KafkaInstrumentation.onConsume(
                kafkaHeaders, "kafka-consumer");

        assertThat(consumerCtx).isNotNull();
        assertThat(consumerCtx.getTraceId()).isEqualTo(traceId);
        assertThat(consumerCtx.getServiceName()).isEqualTo("kafka-consumer");

        KafkaInstrumentation.onConsumeComplete();
    }

    @Test
    @DisplayName("E2E: Should handle mixed HTTP-Kafka trace chain")
    void shouldHandleMixedHttpKafkaChain() {
        // HTTP entry
        TraceContext httpCtx = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "api-gateway");
        String traceId = httpCtx.getTraceId();

        // Gateway produces to Kafka (context already in ThreadLocal from HTTP)
        Map<String, byte[]> kafkaHeaders = new HashMap<>();
        KafkaInstrumentation.onProduce(kafkaHeaders);
        assertThat(new String(kafkaHeaders.get("traceId"), StandardCharsets.UTF_8)).isEqualTo(traceId);

        HttpServletInstrumentation.onHttpRequestComplete();

        // Kafka consumer picks up
        TraceContext consumerCtx = KafkaInstrumentation.onConsume(
                kafkaHeaders, "event-processor");
        assertThat(consumerCtx.getTraceId()).isEqualTo(traceId);

        KafkaInstrumentation.onConsumeComplete();
    }

    @Test
    @DisplayName("E2E: Should isolate parallel request traces")
    void shouldIsolateParallelRequestTraces() {
        // Request 1
        TraceContext ctx1 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-1");
        String traceId1 = ctx1.getTraceId();
        HttpServletInstrumentation.onHttpRequestComplete();

        // Request 2
        TraceContext ctx2 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-2");
        String traceId2 = ctx2.getTraceId();
        HttpServletInstrumentation.onHttpRequestComplete();

        // Traces must be unique
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("E2E: Should generate valid W3C traceparent format")
    void shouldGenerateValidW3CTraceparent() {
        TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        Map<String, String> headers = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headers);

        String traceparent = headers.get(LogConstants.TRACE_PARENT_HEADER);
        assertThat(traceparent).isNotNull();

        // Validate W3C format: version-traceId-spanId-flags
        String[] parts = traceparent.split("-");
        assertThat(parts).hasSize(4);
        assertThat(parts[0]).isEqualTo("00"); // version
        assertThat(parts[1]).hasSize(32);     // traceId (128-bit hex)
        assertThat(parts[2]).hasSize(16);     // spanId (64-bit hex)
        assertThat(parts[3]).matches("0[01]"); // flags (sampled)

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("E2E: Should handle error in trace chain gracefully")
    void shouldHandleErrorInTraceChainGracefully() {
        // Start a trace
        TraceContext ctx = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "error-service");

        String traceId = ctx.getTraceId();

        // Even if an error occurs, context should remain valid
        assertThat(ContextHolder.getTraceId()).isEqualTo(traceId);

        // Cleanup should still work
        HttpServletInstrumentation.onHttpRequestComplete();
        assertThat(ContextHolder.isSet()).isFalse();
    }

    @Test
    @DisplayName("E2E: Should propagate traceparent with sampled=00 for unsampled")
    void shouldPropagateUnsampledTraceparent() {
        // Create a context and mark as unsampled
        TraceContext ctx = new TraceContext();
        ctx.setTraceId(TraceIdGenerator.generateTraceId());
        ctx.setSpanId(TraceIdGenerator.generateSpanId());
        ctx.setServiceName("unsampled-service");
        ctx.setSampled(false);
        ContextHolder.set(ctx);
        MdcLogInjector.injectTraceContext(ctx);

        Map<String, String> headers = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headers);

        String traceparent = headers.get(LogConstants.TRACE_PARENT_HEADER);
        assertThat(traceparent).isNotNull();
        assertThat(traceparent).endsWith("-00"); // unsampled

        HttpServletInstrumentation.onHttpRequestComplete();
    }
}
