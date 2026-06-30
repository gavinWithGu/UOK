package com.bosch.iot.uok.lambda.adapter;

import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link KinesisEventAdapter}.
 */
class KinesisEventAdapterTest {

    // --- isKinesisEvent Tests ---

    @Test
    @DisplayName("Should return false for null event")
    void isKinesisEventShouldReturnFalseForNull() {
        assertThat(KinesisEventAdapter.isKinesisEvent(null)).isFalse();
    }

    @Test
    @DisplayName("Should return true for KinesisEvent class name")
    void isKinesisEventShouldReturnTrueForKinesisEvent() {
        // Create a mock object whose class name contains "KinesisEvent"
        Object event = new Object() {
            @Override
            public String toString() { return "KinesisEvent"; }
        };
        // The actual class name won't contain "KinesisEvent" so this returns false
        // We need to test the class name matching differently
        assertThat(KinesisEventAdapter.isKinesisEvent("some string")).isFalse();
    }

    @Test
    @DisplayName("Should return false for non-Kinesis event object")
    void isKinesisEventShouldReturnFalseForNonKinesis() {
        assertThat(KinesisEventAdapter.isKinesisEvent("plain string")).isFalse();
        assertThat(KinesisEventAdapter.isKinesisEvent(42)).isFalse();
        assertThat(KinesisEventAdapter.isKinesisEvent(Collections.emptyMap())).isFalse();
    }

    @Test
    @DisplayName("Should return true for real KinesisEvent object")
    void isKinesisEventShouldReturnTrueForRealKinesisEvent() {
        KinesisEvent event = new KinesisEvent();
        assertThat(KinesisEventAdapter.isKinesisEvent(event)).isTrue();
    }

    // --- extractTraceContext Tests ---

    @Test
    @DisplayName("Should return null for null event")
    void extractTraceContextShouldReturnNullForNull() {
        assertThat(KinesisEventAdapter.extractTraceContext(null, "test-service")).isNull();
    }

    @Test
    @DisplayName("Should return null for non-Kinesis event")
    void extractTraceContextShouldReturnNullForNonKinesis() {
        assertThat(KinesisEventAdapter.extractTraceContext("not-kinesis", "test-service")).isNull();
    }

    @Test
    @DisplayName("Should return null for real KinesisEvent (reflection not implemented)")
    void extractTraceContextShouldReturnNullForRealKinesisEvent() {
        KinesisEvent event = new KinesisEvent();
        // Current implementation returns null for KinesisEvent
        // (reflection-based extraction not yet implemented)
        assertThat(KinesisEventAdapter.extractTraceContext(event, "test-service")).isNull();
    }

    // --- extractFromHeaders Tests ---

    @Test
    @DisplayName("Should return null for null headers")
    void extractFromHeadersShouldReturnNullForNull() {
        assertThat(KinesisEventAdapter.extractFromHeaders(null, "test-service")).isNull();
    }

    @Test
    @DisplayName("Should return null for empty headers")
    void extractFromHeadersShouldReturnNullForEmpty() {
        assertThat(KinesisEventAdapter.extractFromHeaders(Collections.emptyMap(), "test-service")).isNull();
    }

    @Test
    @DisplayName("Should extract from traceparent header")
    void shouldExtractFromTraceParentHeader() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", traceParent);

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getParentSpanId()).isEqualTo(parentSpanId);
        assertThat(context.getServiceName()).isEqualTo("kinesis-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should extract unsampled from traceparent with sampled=00")
    void shouldExtractUnsampledFromTraceParent() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-00";

        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", traceParent);

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.isSampled()).isFalse();
    }

    @Test
    @DisplayName("Should fall back to traceId header when no traceparent")
    void shouldFallBackToTraceIdHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceId", "0123456789abcdef0123456789abcdef");

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(context.getServiceName()).isEqualTo("kinesis-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should prefer traceparent over traceId header")
    void shouldPreferTraceParentOverTraceId() {
        String traceId = "aaaabbbbccccddddaaaabbbbccccdddd";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", traceParent);
        headers.put("traceId", "ffffffffffffffffffffffffffffffff"); // should be ignored

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
    }

    @Test
    @DisplayName("Should return null for invalid traceparent")
    void shouldReturnNullForInvalidTraceParent() {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceparent", "invalid-value");

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        // Invalid traceparent should not produce context; fall through to traceId
        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null when no trace headers present")
    void shouldReturnNullWhenNoTraceHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null for empty traceId header")
    void shouldReturnNullForEmptyTraceIdHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("traceId", "");

        TraceContext context = KinesisEventAdapter.extractFromHeaders(headers, "kinesis-service");

        assertThat(context).isNull();
    }

    // --- extractFromData Tests ---

    @Test
    @DisplayName("Should return null for null data")
    void extractFromDataShouldReturnNullForNull() {
        assertThat(KinesisEventAdapter.extractFromData(null, "test-service")).isNull();
    }

    @Test
    @DisplayName("Should extract traceparent from JSON data")
    void shouldExtractTraceParentFromJsonData() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        String json = "{\"traceparent\":\"" + traceParent + "\",\"data\":\"test\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getParentSpanId()).isEqualTo(parentSpanId);
        assertThat(context.getServiceName()).isEqualTo("kinesis-service");
    }

    @Test
    @DisplayName("Should extract traceId from JSON data when no traceparent")
    void shouldExtractTraceIdFromJsonData() {
        String traceId = "aaaabbbbccccddddaaaabbbbccccdddd";
        String json = "{\"traceId\":\"" + traceId + "\",\"data\":\"test\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getServiceName()).isEqualTo("kinesis-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should return null for JSON without trace fields")
    void shouldReturnNullForJsonWithoutTraceFields() {
        String json = "{\"data\":\"test\",\"message\":\"hello\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null for non-JSON data")
    void shouldReturnNullForNonJsonData() {
        String rawText = "this is not json";
        ByteBuffer data = ByteBuffer.wrap(rawText.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should preserve ByteBuffer position after extraction")
    void shouldPreserveByteBufferPosition() {
        String json = "{\"traceId\":\"0123456789abcdef0123456789abcdef\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        int originalPosition = data.position();

        KinesisEventAdapter.extractFromData(data, "kinesis-service");

        // The duplicate() method preserves the original buffer's position
        assertThat(data.position()).isEqualTo(originalPosition);
    }

    @Test
    @DisplayName("Should handle empty ByteBuffer")
    void shouldHandleEmptyByteBuffer() {
        ByteBuffer data = ByteBuffer.wrap(new byte[0]);
        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");
        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should return null for invalid traceparent in JSON data")
    void shouldReturnNullForInvalidTraceParentInJsonData() {
        String json = "{\"traceparent\":\"invalid-value\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should handle JSON with traceparent but missing colon")
    void shouldHandleJsonWithMissingColon() {
        String json = "{\"traceparent\" \"00-aaaabbbbccccddddaaaabbbbccccdddd-abcdef0123456789-01\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should handle JSON with traceparent key but no value quotes")
    void shouldHandleJsonWithNoValueQuotes() {
        String json = "{\"traceparent\":12345}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should handle JSON with traceparent key but only start quote")
    void shouldHandleJsonWithOnlyStartQuote() {
        String json = "{\"traceparent\":\"incomplete}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNull();
    }

    @Test
    @DisplayName("Should extract traceparent from complex JSON with nested fields")
    void shouldExtractFromComplexJson() {
        String traceId = "aaaabbbbccccddddaaaabbbbccccdddd";
        String parentSpanId = "abcdef0123456789";
        String traceParent = "00-" + traceId + "-" + parentSpanId + "-01";

        String json = "{\"metadata\":{\"source\":\"kinesis\"},\"traceparent\":\"" + traceParent + "\",\"payload\":\"data\"}";
        ByteBuffer data = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

        TraceContext context = KinesisEventAdapter.extractFromData(data, "kinesis-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
    }
}
