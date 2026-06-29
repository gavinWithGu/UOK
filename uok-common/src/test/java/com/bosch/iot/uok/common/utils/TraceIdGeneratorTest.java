package com.bosch.iot.uok.common.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TraceIdGenerator}.
 */
class TraceIdGeneratorTest {

    @Test
    @DisplayName("Should generate valid 128-bit trace ID (32 hex chars)")
    void shouldGenerateValidTraceId() {
        String traceId = TraceIdGenerator.generateTraceId();
        assertThat(traceId).isNotNull();
        assertThat(traceId.length()).isEqualTo(32);
        assertThat(TraceIdGenerator.isValidTraceId(traceId)).isTrue();
    }

    @Test
    @DisplayName("Should generate valid 64-bit span ID (16 hex chars)")
    void shouldGenerateValidSpanId() {
        String spanId = TraceIdGenerator.generateSpanId();
        assertThat(spanId).isNotNull();
        assertThat(spanId.length()).isEqualTo(16);
        assertThat(TraceIdGenerator.isValidSpanId(spanId)).isTrue();
    }

    @RepeatedTest(10)
    @DisplayName("Should generate unique trace IDs")
    void shouldGenerateUniqueTraceIds() {
        String id1 = TraceIdGenerator.generateTraceId();
        String id2 = TraceIdGenerator.generateTraceId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @RepeatedTest(10)
    @DisplayName("Should generate unique span IDs")
    void shouldGenerateUniqueSpanIds() {
        String id1 = TraceIdGenerator.generateSpanId();
        String id2 = TraceIdGenerator.generateSpanId();
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Should generate non-zero trace IDs")
    void shouldGenerateNonZeroTraceIds() {
        for (int i = 0; i < 100; i++) {
            String traceId = TraceIdGenerator.generateTraceId();
            assertThat(traceId).isNotEqualTo("00000000000000000000000000000000");
        }
    }

    @Test
    @DisplayName("Should validate correct trace ID format")
    void shouldValidateCorrectTraceId() {
        assertThat(TraceIdGenerator.isValidTraceId("0123456789abcdef0123456789abcdef")).isTrue();
        assertThat(TraceIdGenerator.isValidTraceId("ABCDEF0123456789abcdef0123456789")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid trace ID formats")
    void shouldRejectInvalidTraceIdFormats() {
        assertThat(TraceIdGenerator.isValidTraceId(null)).isFalse();
        assertThat(TraceIdGenerator.isValidTraceId("")).isFalse();
        assertThat(TraceIdGenerator.isValidTraceId("too-short")).isFalse();
        assertThat(TraceIdGenerator.isValidTraceId("0123456789abcdef")).isFalse(); // Only 16 chars
        assertThat(TraceIdGenerator.isValidTraceId("00000000000000000000000000000000")).isFalse(); // All zeros
        assertThat(TraceIdGenerator.isValidTraceId("g123456789abcdef0123456789abcdef")).isFalse(); // Non-hex char
    }

    @Test
    @DisplayName("Should validate correct span ID format")
    void shouldValidateCorrectSpanId() {
        assertThat(TraceIdGenerator.isValidSpanId("0123456789abcdef")).isTrue();
        assertThat(TraceIdGenerator.isValidSpanId("ABCDEF0123456789")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid span ID formats")
    void shouldRejectInvalidSpanIdFormats() {
        assertThat(TraceIdGenerator.isValidSpanId(null)).isFalse();
        assertThat(TraceIdGenerator.isValidSpanId("")).isFalse();
        assertThat(TraceIdGenerator.isValidSpanId("too-short")).isFalse();
        assertThat(TraceIdGenerator.isValidSpanId("0123456789abcdef0123456789abcdef")).isFalse(); // Too long
        assertThat(TraceIdGenerator.isValidSpanId("0000000000000000")).isFalse(); // All zeros
        assertThat(TraceIdGenerator.isValidSpanId("g123456789abcdef")).isFalse(); // Non-hex char
    }

    @Test
    @DisplayName("Should format traceparent header correctly")
    void shouldFormatTraceParent() {
        String traceParent = TraceIdGenerator.formatTraceParent(
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                true);

        assertThat(traceParent).isEqualTo("00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");
    }

    @Test
    @DisplayName("Should format traceparent with unsampled flag")
    void shouldFormatTraceParentUnsampled() {
        String traceParent = TraceIdGenerator.formatTraceParent(
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                false);

        assertThat(traceParent).endsWith("-00");
    }

    @Test
    @DisplayName("Should parse valid traceparent header")
    void shouldParseValidTraceParent() {
        String[] parts = TraceIdGenerator.parseTraceParent(
                "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01");

        assertThat(parts).isNotNull();
        assertThat(parts.length).isEqualTo(4);
        assertThat(parts[0]).isEqualTo("00"); // version
        assertThat(parts[1]).isEqualTo("0123456789abcdef0123456789abcdef"); // traceId
        assertThat(parts[2]).isEqualTo("0123456789abcdef"); // spanId
        assertThat(parts[3]).isEqualTo("01"); // flags
    }

    @Test
    @DisplayName("Should reject invalid traceparent headers")
    void shouldRejectInvalidTraceParent() {
        assertThat(TraceIdGenerator.parseTraceParent(null)).isNull();
        assertThat(TraceIdGenerator.parseTraceParent("")).isNull();
        assertThat(TraceIdGenerator.parseTraceParent("invalid")).isNull();
        assertThat(TraceIdGenerator.parseTraceParent("01-0123456789abcdef0123456789abcdef-0123456789abcdef-01")).isNull(); // Wrong version
        assertThat(TraceIdGenerator.parseTraceParent("00-invalid-trace-id-invalid-01")).isNull();
        assertThat(TraceIdGenerator.parseTraceParent("00-0123456789abcdef0123456789abcdef-invalid-01")).isNull();
    }

    @Test
    @DisplayName("Should generate and validate round-trip trace context")
    void shouldRoundTripTraceContext() {
        String traceId = TraceIdGenerator.generateTraceId();
        String spanId = TraceIdGenerator.generateSpanId();

        String traceParent = TraceIdGenerator.formatTraceParent(traceId, spanId, true);
        String[] parsed = TraceIdGenerator.parseTraceParent(traceParent);

        assertThat(parsed).isNotNull();
        assertThat(parsed[1]).isEqualTo(traceId);
        assertThat(parsed[2]).isEqualTo(spanId);
    }
}
