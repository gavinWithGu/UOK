package com.bosch.iot.uok.lambda;

import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.lambda.context.LambdaContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LambdaTracingInitializer}.
 */
class LambdaTracingInitializerTest {

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
    @DisplayName("Should initialize successfully")
    void shouldInitialize() {
        LambdaTracingInitializer.initialize();
        assertThat(LambdaTracingInitializer.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("Should create root context when no event provided")
    void shouldCreateRootContextForNullEvent() {
        LambdaTracingInitializer.initialize();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.getSpanId()).isNotNull();
        assertThat(context.getServiceName()).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should inject trace context into MDC")
    void shouldInjectIntoMdc() {
        LambdaTracingInitializer.initialize();

        LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(MDC.get("traceId")).isNotNull();
        assertThat(MDC.get("spanId")).isNotNull();
        assertThat(MDC.get("serviceName")).isNotNull();
    }

    @Test
    @DisplayName("Should set context in LambdaContextHolder")
    void shouldSetInLambdaContextHolder() {
        LambdaTracingInitializer.initialize();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(LambdaContextHolder.isSet()).isTrue();
        assertThat(LambdaContextHolder.getTraceId()).isEqualTo(context.getTraceId());
    }

    @Test
    @DisplayName("Should clean up on lambda complete")
    void shouldCleanUpOnComplete() {
        LambdaTracingInitializer.initialize();
        LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(LambdaContextHolder.isSet()).isTrue();

        LambdaTracingInitializer.onLambdaComplete();

        assertThat(LambdaContextHolder.isSet()).isFalse();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("Should auto-initialize on onLambdaEvent if not explicitly initialized")
    void shouldAutoInitialize() {
        // Don't call initialize() first
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(context).isNotNull();
        assertThat(LambdaTracingInitializer.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("Should get config after initialization")
    void shouldGetConfig() {
        LambdaTracingInitializer.initialize();
        assertThat(LambdaTracingInitializer.getConfig()).isNotNull();
        assertThat(LambdaTracingInitializer.getConfig().getServiceName()).isNotNull();
    }

    @Test
    @DisplayName("Should get tracer after initialization")
    void shouldGetTracer() {
        LambdaTracingInitializer.initialize();
        assertThat(LambdaTracingInitializer.getTracer()).isNotNull();
    }

    @Test
    @DisplayName("Should handle unknown event type gracefully")
    void shouldHandleUnknownEventType() {
        LambdaTracingInitializer.initialize();

        // Pass a String which is not a Kinesis or Kafka event
        TraceContext context = LambdaTracingInitializer.onLambdaEvent("some-string-event");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should extract context from Kinesis headers map")
    void shouldExtractFromKinesisHeaders() {
        LambdaTracingInitializer.initialize();

        // Use the adapter directly
        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");

        TraceContext context = com.bosch.iot.uok.lambda.adapter.KinesisEventAdapter
                .extractFromHeaders(headers, "lambda-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
    }

    @Test
    @DisplayName("Should extract context from Kafka headers map")
    void shouldExtractFromKafkaHeaders() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));

        TraceContext context = com.bosch.iot.uok.lambda.adapter.KafkaEventAdapter
                .extractFromHeaders(headers, "lambda-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
    }
}
