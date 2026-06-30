package com.bosch.iot.uok.lambda.adapter;

import com.amazonaws.services.lambda.runtime.events.KafkaEvent;
import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KafkaEventAdapter}.
 */
class KafkaEventAdapterTest {

    // --- isKafkaEvent Tests ---

    @Test
    @DisplayName("Should return false for null event")
    void isKafkaEventShouldReturnFalseForNull() {
        assertThat(KafkaEventAdapter.isKafkaEvent(null)).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-Kafka event object")
    void isKafkaEventShouldReturnFalseForNonKafka() {
        assertThat(KafkaEventAdapter.isKafkaEvent("plain string")).isFalse();
        assertThat(KafkaEventAdapter.isKafkaEvent(42)).isFalse();
        assertThat(KafkaEventAdapter.isKafkaEvent(Collections.emptyMap())).isFalse();
    }

    @Test
    @DisplayName("Should return true for real KafkaEvent object")
    void isKafkaEventShouldReturnTrueForRealKafkaEvent() {
        KafkaEvent event = new KafkaEvent();
        assertThat(KafkaEventAdapter.isKafkaEvent(event)).isTrue();
    }

    // --- extractTraceContext Tests ---

    @Test
    @DisplayName("Should return null for any event (reflection not implemented)")
    void extractTraceContextShouldReturnNull() {
        assertThat(KafkaEventAdapter.extractTraceContext("event", "test-service")).isNull();
        assertThat(KafkaEventAdapter.extractTraceContext(null, "test-service")).isNull();
    }

    // --- extractFromHeaders Tests ---

    @Test
    @DisplayName("Should return null for null headers")
    void extractFromHeadersShouldReturnNullForNull() {
        assertThat(KafkaEventAdapter.extractFromHeaders(null, "test-service")).isNull();
    }

    @Test
    @DisplayName("Should return null for empty headers")
    void extractFromHeadersShouldReturnNullForEmpty() {
        assertThat(KafkaEventAdapter.extractFromHeaders(Collections.emptyMap(), "test-service")).isNull();
    }

    @Test
    @DisplayName("Should extract from traceparent header (byte[] value)")
    void shouldExtractFromTraceParentHeader() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getParentSpanId()).isEqualTo(parentSpanId);
        assertThat(context.getServiceName()).isEqualTo("kafka-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should extract unsampled from traceparent with sampled=00")
    void shouldExtractUnsampledFromTraceParent() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-00";

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNotNull();
        assertThat(context.isSampled()).isFalse();
    }

    @Test
    @DisplayName("Should fall back to traceId header when no traceparent")
    void shouldFallBackToTraceIdHeader() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceId", "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(context.getServiceName()).isEqualTo("kafka-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should prefer traceparent over traceId header")
    void shouldPreferTraceParentOverTraceId() {
        String traceId = "aaaabbbbccccddddaaaabbbbccccdddd";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));
        headers.put("traceId", "ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should return null for invalid traceparent")
    void shouldReturnNullForInvalidTraceParent() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", "invalid-value".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        // Invalid traceparent falls through to traceId check, which is absent -> null
        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null when no trace headers present")
    void shouldReturnNullWhenNoTraceHeaders() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put("content-type", "application/json".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null for empty traceId header")
    void shouldReturnNullForEmptyTraceIdHeader() {
        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceId", "".getBytes(StandardCharsets.UTF_8));

        TraceContext context = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should generate unique span ID for each extraction")
    void shouldGenerateUniqueSpanId() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String traceParent = "00-" + traceId + "-abcdef0123456789-01";

        Map<String, byte[]> headers = new HashMap<>();
        headers.put("traceparent", traceParent.getBytes(StandardCharsets.UTF_8));

        TraceContext ctx1 = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");
        TraceContext ctx2 = KafkaEventAdapter.extractFromHeaders(headers, "kafka-service");

        assertThat(ctx1).isNotNull();
        assertThat(ctx2).isNotNull();
        // Each extraction should generate a new span ID
        assertThat(ctx1.getSpanId()).isNotEqualTo(ctx2.getSpanId());
    }
}
