package com.bosch.iot.uok.agent.instrumentation.http;

import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HttpServletInstrumentation}.
 */
class HttpServletInstrumentationTest {

    @BeforeEach
    void setUp() {
        ContextHolder.remove();
        MdcLogInjector.initialize(new UokConfig());
    }

    @AfterEach
    void tearDown() {
        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("Should create root context when no traceparent header")
    void shouldCreateRootContextWhenNoTraceparent() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.getSpanId()).isNotNull();
        assertThat(context.getServiceName()).isEqualTo("test-service");
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should create root context when empty traceparent header")
    void shouldCreateRootContextWhenEmptyTraceparent() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                "", Collections.emptyMap(), "test-service");

        assertThat(context).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should extract context from valid traceparent header")
    void shouldExtractContextFromValidTraceparent() {
        // Generate a valid traceparent
        String traceId = "0123456789abcdef0123456789abcdef";
        String spanId = "0123456789abcdef";
        String traceParent = "00-" + traceId + "-" + spanId + "-01";

        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                traceParent, Collections.emptyMap(), "downstream-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getServiceName()).isEqualTo("downstream-service");
        // Should be a child span
        assertThat(context.getParentSpanId()).isEqualTo(spanId);
    }

    @Test
    @DisplayName("Should handle invalid traceparent header gracefully")
    void shouldHandleInvalidTraceparentGracefully() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                "invalid-traceparent", Collections.emptyMap(), "test-service");

        assertThat(context).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should inject trace context into ThreadLocal and MDC")
    void shouldInjectIntoThreadLocalAndMdc() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        assertThat(ContextHolder.get()).isNotNull();
        assertThat(ContextHolder.getTraceId()).isEqualTo(context.getTraceId());
        assertThat(org.slf4j.MDC.get(LogConstants.TRACE_ID)).isEqualTo(context.getTraceId());
    }

    @Test
    @DisplayName("Should inject business fields from headers")
    void shouldInjectBusinessFieldsFromHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("deviceId", "device-123");
        headers.put("userId", "user-456");

        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, headers, "test-service");

        assertThat(org.slf4j.MDC.get("deviceId")).isEqualTo("device-123");
        assertThat(org.slf4j.MDC.get("userId")).isEqualTo("user-456");
    }

    @Test
    @DisplayName("Should inject trace context into outgoing response headers")
    void shouldInjectIntoOutgoingHeaders() {
        HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        Map<String, String> outgoingHeaders = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(outgoingHeaders);

        assertThat(outgoingHeaders).containsKey(LogConstants.TRACE_PARENT_HEADER);
        assertThat(outgoingHeaders.get(LogConstants.TRACE_PARENT_HEADER)).startsWith("00-");
    }

    @Test
    @DisplayName("Should handle null headers in onHttpResponse")
    void shouldHandleNullHeadersInResponse() {
        HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        // Should not throw
        HttpServletInstrumentation.onHttpResponse(null);
    }

    @Test
    @DisplayName("Should clean up context on request complete")
    void shouldCleanUpOnRequestComplete() {
        HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        assertThat(ContextHolder.isSet()).isTrue();

        HttpServletInstrumentation.onHttpRequestComplete();

        assertThat(ContextHolder.isSet()).isFalse();
        assertThat(org.slf4j.MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Should handle null context in onHttpResponse")
    void shouldHandleNullContextInResponse() {
        ContextHolder.remove();
        Map<String, String> headers = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headers);
        assertThat(headers).isEmpty();
    }
}
