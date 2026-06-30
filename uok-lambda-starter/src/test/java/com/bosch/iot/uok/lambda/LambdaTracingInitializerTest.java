package com.bosch.iot.uok.lambda;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
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
    @DisplayName("Should not double-initialize")
    void shouldNotDoubleInitialize() {
        LambdaTracingInitializer.initialize();
        LambdaTracingInitializer.initialize(); // second call should be no-op
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
        assertThat(MDC.get("spanId")).isNull();
        assertThat(MDC.get("serviceName")).isNull();
        assertThat(MDC.get("env")).isNull();
        assertThat(MDC.get("bizDomain")).isNull();
        assertThat(MDC.get("teamName")).isNull();
    }

    @Test
    @DisplayName("Should auto-initialize on onLambdaEvent if not explicitly initialized")
    void shouldAutoInitialize() {
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
    @DisplayName("Should get OpenTelemetry instance after initialization")
    void shouldGetOpenTelemetry() {
        LambdaTracingInitializer.initialize();
        assertThat(LambdaTracingInitializer.getOpenTelemetry()).isNotNull();
    }

    @Test
    @DisplayName("Should handle unknown event type gracefully")
    void shouldHandleUnknownEventType() {
        LambdaTracingInitializer.initialize();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent("some-string-event");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should extract context from Kinesis headers map")
    void shouldExtractFromKinesisHeaders() {
        LambdaTracingInitializer.initialize();

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

    @Test
    @DisplayName("Should handle Kinesis-like event via onLambdaEvent")
    void shouldHandleKinesisLikeEvent() {
        LambdaTracingInitializer.initialize();

        // Use a real KinesisEvent class to trigger the isKinesisEvent branch
        KinesisEvent kinesisEvent = new KinesisEvent();
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(kinesisEvent);

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
    }

    @Test
    @DisplayName("Should handle Kafka-like event via onLambdaEvent")
    void shouldHandleKafkaLikeEvent() {
        LambdaTracingInitializer.initialize();

        // Use a real KafkaEvent class to trigger the isKafkaEvent branch
        KafkaEvent kafkaEvent = new KafkaEvent();
        TraceContext context = LambdaTracingInitializer.onLambdaEvent(kafkaEvent);

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
    }

    @Test
    @DisplayName("Should propagate context fields from config")
    void shouldPropagateContextFieldsFromConfig() {
        LambdaTracingInitializer.initialize();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(context.getServiceName()).isNotNull();
        assertThat(context.getEnv()).isNotNull();
        assertThat(context.getBizDomain()).isNotNull();
        assertThat(context.getTeamName()).isNotNull();
    }

    @Test
    @DisplayName("Should mark root context as sampled")
    void shouldMarkRootContextAsSampled() {
        LambdaTracingInitializer.initialize();

        TraceContext context = LambdaTracingInitializer.onLambdaEvent(null);

        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should handle multiple sequential events")
    void shouldHandleMultipleSequentialEvents() {
        LambdaTracingInitializer.initialize();

        TraceContext ctx1 = LambdaTracingInitializer.onLambdaEvent(null);
        String traceId1 = ctx1.getTraceId();

        LambdaTracingInitializer.onLambdaComplete();
        assertThat(LambdaContextHolder.isSet()).isFalse();

        TraceContext ctx2 = LambdaTracingInitializer.onLambdaEvent(null);
        String traceId2 = ctx2.getTraceId();

        // Each event should get a unique trace ID
        assertThat(traceId2).isNotEqualTo(traceId1);
    }

    @Test
    @DisplayName("onLambdaComplete should be idempotent")
    void onLambdaCompleteShouldBeIdempotent() {
        LambdaTracingInitializer.initialize();
        LambdaTracingInitializer.onLambdaEvent(null);

        LambdaTracingInitializer.onLambdaComplete();
        LambdaTracingInitializer.onLambdaComplete(); // second call should not throw

        assertThat(LambdaContextHolder.isSet()).isFalse();
    }
}
