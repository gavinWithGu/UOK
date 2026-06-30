package com.bosch.iot.uok.integration;

import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.lambda.LambdaTracingInitializer;
import com.bosch.iot.uok.lambda.adapter.KafkaEventAdapter;
import com.bosch.iot.uok.lambda.adapter.KinesisEventAdapter;
import com.bosch.iot.uok.lambda.context.LambdaContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Lambda event tracing.
 * Tests the complete Lambda event processing pipeline
 * with Kinesis and Kafka events.
 */
class LambdaTracingIntegrationTest {

    @BeforeEach
    void setUp() {
        LambdaContextHolder.clear();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        LambdaTracingInitializer.onLambdaComplete();
    }

    @Test
    @DisplayName("Lambda E2E: Should process Kinesis event end-to-end")
    void shouldProcessKinesisEventEndToEnd() {
        LambdaTracingInitializer.initialize();

        // Simulate Kinesis event
        KinesisEvent kinesisEvent = new KinesisEvent();

        // onLambdaEvent recognizes it as KinesisEvent
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(kinesisEvent);

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(LambdaContextHolder.isSet()).isTrue();
        assertThat(MDC.get("traceId")).isNotNull();
    }

    @Test
    @DisplayName("Lambda E2E: Should process Kafka event end-to-end")
    void shouldProcessKafkaEventEndToEnd() {
        LambdaTracingInitializer.initialize();

        KafkaEvent kafkaEvent = new KafkaEvent();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent(kafkaEvent);

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(LambdaContextHolder.isSet()).isTrue();
    }

    @Test
    @DisplayName("Lambda E2E: Should extract trace from Kinesis headers and propagate to Lambda context")
    void shouldExtractFromKinesisHeadersAndPropagate() {
        LambdaTracingInitializer.initialize();

        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", traceParent);

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "lambda-handler");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getParentSpanId()).isEqualTo(parentSpanId);

        // Propagate into Lambda context
        LambdaContextHolder.set(context);
        assertThat(LambdaContextHolder.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Lambda E2E: Should extract trace from Kafka headers and propagate to Lambda context")
    void shouldExtractFromKafkaHeadersAndPropagate() {
        LambdaTracingInitializer.initialize();

        String traceId = "aaaabbbbccccddddaaaabbbbccccdddd";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "lambda-handler");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);

        LambdaContextHolder.set(context);
        assertThat(LambdaContextHolder.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Lambda E2E: Should handle Lambda lifecycle - init, event, complete")
    void shouldHandleLambdaLifecycle() {
        // Initialize
        LambdaTracingInitializer.initialize();
        assertThat(LambdaTracingInitializer.isInitialized()).isTrue();

        // Process event
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);
        assertThat(context).isNotNull();
        assertThat(LambdaContextHolder.isSet()).isTrue();
        assertThat(MDC.get("traceId")).isNotNull();

        // Complete
        LambdaTracingInitializer.onLambdaComplete();
        assertThat(LambdaContextHolder.isSet()).isFalse();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("Lambda E2E: Should handle multiple sequential Lambda invocations")
    void shouldHandleMultipleInvocations() {
        LambdaTracingInitializer.initialize();

        // Invocation 1
        TraceContext ctx1 = LambdaTracingInitializer.onLambdaEvent(null);
        String traceId1 = ctx1.getTraceId();
        LambdaTracingInitializer.onLambdaComplete();

        // Invocation 2
        TraceContext ctx2 = LambdaTracingInitializer.onLambdaEvent(null);
        String traceId2 = ctx2.getTraceId();
        LambdaTracingInitializer.onLambdaComplete();

        // Each invocation should get a unique trace ID
        assertThat(traceId1).isNotEqualTo(traceId2);
    }

    @Test
    @DisplayName("Lambda E2E: Should auto-initialize when not explicitly initialized")
    void shouldAutoInitialize() {
        // Don't call initialize() first
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(context).isNotNull();
        assertThat(LambdaTracingInitializer.isInitialized()).isTrue();
        assertThat(LambdaContextHolder.isSet()).isTrue();
    }
}
