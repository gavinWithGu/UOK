package com.bosch.iot.uok.agent.instrumentation.kafka;

import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
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
 * Unit tests for {@link KafkaInstrumentation}.
 */
class KafkaInstrumentationTest {

    @BeforeEach
    void setUp() {
        ContextHolder.remove();
        MdcLogInjector.initialize(new UokConfig());
    }

    @AfterEach
    void tearDown() {
        KafkaInstrumentation.onConsumeComplete();
    }

    @Test
    @DisplayName("Should inject trace context into Kafka producer headers")
    void shouldInjectIntoProducerHeaders() {
        TraceContext context = new TraceContext("trace-123", "span-456", "producer-service");
        context.setParentSpanId("parent-789");
        context.setSampled(true);
        ContextHolder.set(context);

        Map<String, byte[]> headers = new HashMap<>();
        KafkaInstrumentation.onProduce(headers);

        assertThat(new String(headers.get(LogConstants.KAFKA_TRACE_ID_KEY), StandardCharsets.UTF_8))
                .isEqualTo("trace-123");
        assertThat(new String(headers.get(LogConstants.KAFKA_SPAN_ID_KEY), StandardCharsets.UTF_8))
                .isEqualTo("span-456");
        assertThat(new String(headers.get(LogConstants.KAFKA_SAMPLED_KEY), StandardCharsets.UTF_8))
                .isEqualTo("true");
    }

    @Test
    @DisplayName("Should handle null headers in producer")
    void shouldHandleNullProducerHeaders() {
        TraceContext context = new TraceContext("trace-1", "span-1", "svc");
        ContextHolder.set(context);
        KafkaInstrumentation.onProduce(null);
        // Should not throw
    }

    @Test
    @DisplayName("Should extract trace context from Kafka consumer headers")
    void shouldExtractFromConsumerHeaders() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put(LogConstants.KAFKA_TRACE_ID_KEY, "trace-123".getBytes(StandardCharsets.UTF_8));
        headers.put(LogConstants.KAFKA_SPAN_ID_KEY, "span-456".getBytes(StandardCharsets.UTF_8));
        headers.put(LogConstants.KAFKA_SAMPLED_KEY, "true".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaInstrumentation.onConsume(headers, "consumer-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("trace-123");
        assertThat(context.getParentSpanId()).isEqualTo("span-456");
        assertThat(context.getServiceName()).isEqualTo("consumer-service");
    }

    @Test
    @DisplayName("Should create root context when no headers present")
    void shouldCreateRootContextWhenNoHeaders() {
        TraceContext context = KafkaInstrumentation.onConsume(null, "consumer-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should inject consumer context into MDC")
    void shouldInjectConsumerContextIntoMdc() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put(LogConstants.KAFKA_TRACE_ID_KEY, "trace-abc".getBytes(StandardCharsets.UTF_8));
        headers.put(LogConstants.KAFKA_SPAN_ID_KEY, "span-def".getBytes(StandardCharsets.UTF_8));

        KafkaInstrumentation.onConsume(headers, "consumer-service");

        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo("trace-abc");
    }

    @Test
    @DisplayName("Should clean up context on consume complete")
    void shouldCleanUpOnConsumeComplete() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put(LogConstants.KAFKA_TRACE_ID_KEY, "trace-1".getBytes(StandardCharsets.UTF_8));
        headers.put(LogConstants.KAFKA_SPAN_ID_KEY, "span-1".getBytes(StandardCharsets.UTF_8));

        KafkaInstrumentation.onConsume(headers, "svc");
        assertThat(ContextHolder.isSet()).isTrue();

        KafkaInstrumentation.onConsumeComplete();
        assertThat(ContextHolder.isSet()).isFalse();
        assertThat(MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Producer should create root context if none exists")
    void shouldCreateRootIfNoContext() {
        ContextHolder.remove();
        Map<String, byte[]> headers = new HashMap<>();
        KafkaInstrumentation.onProduce(headers);

        assertThat(headers).containsKey(LogConstants.KAFKA_TRACE_ID_KEY);
    }
}
