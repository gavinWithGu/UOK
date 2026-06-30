package com.bosch.iot.uok.agent.instrumentation.webflux;

import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ReactiveContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link WebFluxInstrumentation}.
 */
class WebFluxInstrumentationTest {

    @BeforeEach
    void setUp() {
        ReactiveContextHolder.clear();
        MdcLogInjector.initialize(new UokConfig());
    }

    @AfterEach
    void tearDown() {
        WebFluxInstrumentation.onWebFluxRequestComplete();
    }

    @Test
    @DisplayName("Should create root context when no traceparent header")
    void shouldCreateRootContextWhenNoTraceparent() {
        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "webflux-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.getSpanId()).isNotNull();
        assertThat(context.getServiceName()).isEqualTo("webflux-service");
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should create root context when empty traceparent header")
    void shouldCreateRootContextWhenEmptyTraceparent() {
        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                "", Collections.emptyMap(), "webflux-service");

        assertThat(context).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should extract context from valid traceparent header")
    void shouldExtractContextFromValidTraceparent() {
        String traceId = "0123456789abcdef0123456789abcdef";
        String spanId = "0123456789abcdef";
        String traceParent = "00-" + traceId + "-" + spanId + "-01";

        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                traceParent, Collections.emptyMap(), "downstream-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo(traceId);
        assertThat(context.getServiceName()).isEqualTo("downstream-service");
        assertThat(context.getParentSpanId()).isEqualTo(spanId);
    }

    @Test
    @DisplayName("Should handle invalid traceparent header gracefully")
    void shouldHandleInvalidTraceparentGracefully() {
        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                "invalid-traceparent", Collections.emptyMap(), "webflux-service");

        assertThat(context).isNotNull();
        assertThat(context.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should store context in ReactiveContextHolder")
    void shouldStoreInReactiveContextHolder() {
        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "webflux-service");

        assertThat(ReactiveContextHolder.get()).isNotNull();
        assertThat(ReactiveContextHolder.get().getTraceId()).isEqualTo(context.getTraceId());
    }

    @Test
    @DisplayName("Should inject trace context into MDC")
    void shouldInjectIntoMdc() {
        TraceContext context = WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "webflux-service");

        assertThat(org.slf4j.MDC.get(LogConstants.TRACE_ID)).isEqualTo(context.getTraceId());
        assertThat(org.slf4j.MDC.get(LogConstants.SPAN_ID)).isEqualTo(context.getSpanId());
    }

    @Test
    @DisplayName("Should clean up context on request complete")
    void shouldCleanUpOnRequestComplete() {
        WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "webflux-service");

        assertThat(ReactiveContextHolder.get()).isNotNull();

        WebFluxInstrumentation.onWebFluxRequestComplete();

        assertThat(ReactiveContextHolder.get()).isNull();
        assertThat(org.slf4j.MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Should wrap function with context propagation")
    void shouldWrapFunctionWithContextPropagation() {
        TraceContext context = new TraceContext();
        context.setTraceId("test-trace-id-1234");
        context.setSpanId("test-span-id-5678");
        context.setServiceName("webflux-service");
        ReactiveContextHolder.set(context);

        Function<String, String> original = String::toUpperCase;
        Function<String, String> wrapped = WebFluxInstrumentation.wrapFunction(original);

        assertThat(wrapped).isNotNull();
        String result = wrapped.apply("hello");
        assertThat(result).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Wrapped function should restore MDC context")
    void wrappedFunctionShouldRestoreMdcContext() {
        TraceContext context = new TraceContext();
        context.setTraceId("mdc-trace-id-abcd");
        context.setSpanId("mdc-span-id-ef01");
        context.setServiceName("webflux-service");
        ReactiveContextHolder.set(context);

        Function<Void, String> checkMdc = v -> org.slf4j.MDC.get(LogConstants.TRACE_ID);
        Function<Void, String> wrapped = WebFluxInstrumentation.wrapFunction(checkMdc);

        String mdcTraceId = wrapped.apply(null);
        assertThat(mdcTraceId).isEqualTo("mdc-trace-id-abcd");
    }

    @Test
    @DisplayName("Wrapped function should clean up MDC after execution")
    void wrappedFunctionShouldCleanUpMdcAfterExecution() {
        TraceContext context = new TraceContext();
        context.setTraceId("cleanup-trace-id");
        context.setSpanId("cleanup-span-id");
        ReactiveContextHolder.set(context);

        Function<String, String> wrapped = WebFluxInstrumentation.wrapFunction(String::toUpperCase);
        wrapped.apply("test");

        // After wrapped function completes, MDC should be cleared
        assertThat(org.slf4j.MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Should return null when wrapping null function")
    void shouldReturnNullForNullFunction() {
        Function<String, String> result = WebFluxInstrumentation.wrapFunction(null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Wrapped function should handle null reactive context")
    void wrappedFunctionShouldHandleNullReactiveContext() {
        ReactiveContextHolder.clear();

        Function<String, String> original = String::toUpperCase;
        Function<String, String> wrapped = WebFluxInstrumentation.wrapFunction(original);

        // Should not throw, just execute the function without MDC injection
        String result = wrapped.apply("test");
        assertThat(result).isEqualTo("TEST");
    }

    @Test
    @DisplayName("Should handle multiple sequential requests")
    void shouldHandleMultipleSequentialRequests() {
        TraceContext ctx1 = WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "service-1");
        String traceId1 = ctx1.getTraceId();

        WebFluxInstrumentation.onWebFluxRequestComplete();

        assertThat(ReactiveContextHolder.get()).isNull();

        TraceContext ctx2 = WebFluxInstrumentation.onWebFluxRequest(
                null, Collections.emptyMap(), "service-2");
        String traceId2 = ctx2.getTraceId();

        // Each request should get a unique trace ID
        assertThat(traceId2).isNotEqualTo(traceId1);
        assertThat(ctx2.getServiceName()).isEqualTo("service-2");
    }
}
