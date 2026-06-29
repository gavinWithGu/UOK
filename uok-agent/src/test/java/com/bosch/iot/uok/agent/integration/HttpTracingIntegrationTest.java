package com.bosch.iot.uok.agent.integration;

import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for HTTP trace context propagation.
 * Simulates a cross-service call chain: Gateway → Service A → Service B
 * and verifies that traceId/spanId/parentSpanId are correctly propagated.
 */
class HttpTracingIntegrationTest {

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
    @DisplayName("Full chain: Gateway → ServiceA → ServiceB - traceId should be consistent")
    void shouldPropagateTraceIdAcrossFullChain() {
        // === Step 1: Gateway receives request (no traceparent - root span) ===
        TraceContext gatewayContext = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "sample-gateway");

        String traceId = gatewayContext.getTraceId();
        String gatewaySpanId = gatewayContext.getSpanId();

        assertThat(traceId).isNotNull();
        assertThat(gatewaySpanId).isNotNull();
        assertThat(gatewayContext.isRoot()).isTrue();

        // Verify MDC in Gateway
        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo(traceId);
        assertThat(MDC.get(LogConstants.SPAN_ID)).isEqualTo(gatewaySpanId);

        // === Step 2: Gateway sends request to Service A (simulate outgoing headers) ===
        Map<String, String> outgoingHeaders = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(outgoingHeaders);

        String traceParentFromGateway = outgoingHeaders.get(LogConstants.TRACE_PARENT_HEADER);
        assertThat(traceParentFromGateway).isNotNull();
        assertThat(traceParentFromGateway).contains(traceId);

        // Clean up Gateway context
        HttpServletInstrumentation.onHttpRequestComplete();

        // === Step 3: Service A receives request from Gateway ===
        TraceContext serviceAContext = HttpServletInstrumentation.onHttpRequest(
                traceParentFromGateway, Collections.emptyMap(), "sample-service-a");

        // TraceId should be the same across all services
        assertThat(serviceAContext.getTraceId()).isEqualTo(traceId);
        // Service A should have a different spanId
        assertThat(serviceAContext.getSpanId()).isNotEqualTo(gatewaySpanId);
        // Service A's parentSpanId should point to Gateway's spanId
        assertThat(serviceAContext.getParentSpanId()).isEqualTo(gatewaySpanId);

        // Verify MDC in Service A
        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo(traceId);

        // === Step 4: Service A sends request to Service B ===
        outgoingHeaders.clear();
        HttpServletInstrumentation.onHttpResponse(outgoingHeaders);

        String traceParentFromServiceA = outgoingHeaders.get(LogConstants.TRACE_PARENT_HEADER);
        assertThat(traceParentFromServiceA).isNotNull();

        String serviceASpanId = serviceAContext.getSpanId();
        HttpServletInstrumentation.onHttpRequestComplete();

        // === Step 5: Service B receives request from Service A ===
        TraceContext serviceBContext = HttpServletInstrumentation.onHttpRequest(
                traceParentFromServiceA, Collections.emptyMap(), "sample-service-b");

        // TraceId should still be the same
        assertThat(serviceBContext.getTraceId()).isEqualTo(traceId);
        // Service B should have a different spanId
        assertThat(serviceBContext.getSpanId()).isNotEqualTo(gatewaySpanId);
        assertThat(serviceBContext.getSpanId()).isNotEqualTo(serviceASpanId);
        // Service B's parentSpanId should point to Service A's spanId
        assertThat(serviceBContext.getParentSpanId()).isEqualTo(serviceASpanId);

        // Verify MDC in Service B
        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo(traceId);
        assertThat(MDC.get(LogConstants.SPAN_ID)).isEqualTo(serviceBContext.getSpanId());

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("Business fields should propagate through the chain")
    void shouldPropagateBusinessFields() {
        Map<String, String> headers = new HashMap<>();
        headers.put("deviceId", "device-001");
        headers.put("userId", "user-123");

        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, headers, "sample-gateway");

        assertThat(MDC.get("deviceId")).isEqualTo("device-001");
        assertThat(MDC.get("userId")).isEqualTo("user-123");

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("Context cleanup should prevent context leakage between requests")
    void shouldPreventContextLeakage() {
        // First request
        TraceContext ctx1 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-1");
        String traceId1 = ctx1.getTraceId();

        HttpServletInstrumentation.onHttpRequestComplete();

        // Second request - should have a different traceId
        TraceContext ctx2 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-2");
        String traceId2 = ctx2.getTraceId();

        assertThat(traceId1).isNotEqualTo(traceId2);
        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo(traceId2);
        assertThat(MDC.get(LogConstants.TRACE_ID)).isNotEqualTo(traceId1);

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("Error request should still have valid trace context")
    void shouldHandleErrorRequestWithTraceContext() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "error-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.isSampled()).isTrue();

        // Simulate error - trace context should still be available
        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo(context.getTraceId());

        HttpServletInstrumentation.onHttpRequestComplete();
    }

    @Test
    @DisplayName("W3C traceparent format should be correct in outgoing headers")
    void shouldOutputCorrectTraceParentFormat() {
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "test-service");

        Map<String, String> headers = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headers);

        String traceParent = headers.get(LogConstants.TRACE_PARENT_HEADER);
        String[] parts = TraceIdGenerator.parseTraceParent(traceParent);

        assertThat(parts).isNotNull();
        assertThat(parts[1]).isEqualTo(context.getTraceId());
    }

    @Test
    @DisplayName("Parallel requests should have independent trace contexts")
    void shouldHandleParallelRequests() throws Exception {
        // Simulate two concurrent requests
        TraceContext ctx1 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-1");
        String traceId1 = ctx1.getTraceId();
        String spanId1 = ctx1.getSpanId();

        // Save context
        TraceContext captured1 = ContextHolder.capture();

        // Clean and start second request
        HttpServletInstrumentation.onHttpRequestComplete();

        TraceContext ctx2 = HttpServletInstrumentation.onHttpRequest(
                null, Collections.emptyMap(), "service-2");
        String traceId2 = ctx2.getTraceId();
        String spanId2 = ctx2.getSpanId();

        // They should be completely independent
        assertThat(traceId1).isNotEqualTo(traceId2);
        assertThat(spanId1).isNotEqualTo(spanId2);

        HttpServletInstrumentation.onHttpRequestComplete();
    }
}
