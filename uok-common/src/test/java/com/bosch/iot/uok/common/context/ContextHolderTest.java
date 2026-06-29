package com.bosch.iot.uok.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContextHolder}.
 */
class ContextHolderTest {

    @BeforeEach
    void setUp() {
        ContextHolder.remove();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.remove();
    }

    @Test
    @DisplayName("Should set and get trace context")
    void shouldSetAndGetContext() {
        TraceContext ctx = new TraceContext("trace-1", "span-1", "svc");
        ContextHolder.set(ctx);

        assertThat(ContextHolder.get()).isNotNull();
        assertThat(ContextHolder.getTraceId()).isEqualTo("trace-1");
        assertThat(ContextHolder.getSpanId()).isEqualTo("span-1");
    }

    @Test
    @DisplayName("Should return null when no context is set")
    void shouldReturnNullWhenNotSet() {
        assertThat(ContextHolder.get()).isNull();
        assertThat(ContextHolder.getTraceId()).isNull();
        assertThat(ContextHolder.getSpanId()).isNull();
        assertThat(ContextHolder.getParentSpanId()).isNull();
    }

    @Test
    @DisplayName("Should check if context is set")
    void shouldCheckIfSet() {
        assertThat(ContextHolder.isSet()).isFalse();

        ContextHolder.set(new TraceContext("t", "s", "svc"));
        assertThat(ContextHolder.isSet()).isTrue();

        ContextHolder.remove();
        assertThat(ContextHolder.isSet()).isFalse();
    }

    @Test
    @DisplayName("Should remove context and clear MDC")
    void shouldRemoveContextAndMdc() {
        TraceContext ctx = new TraceContext("trace-1", "span-1", "svc");
        ctx.setParentSpanId("parent-1");
        ctx.setEnv("prod");
        ContextHolder.set(ctx);

        assertThat(ContextHolder.isSet()).isTrue();

        ContextHolder.remove();
        assertThat(ContextHolder.isSet()).isFalse();
        assertThat(ContextHolder.getTraceId()).isNull();
    }

    @Test
    @DisplayName("Should capture and restore context")
    void shouldCaptureAndRestoreContext() {
        TraceContext original = new TraceContext("trace-1", "span-1", "svc-a");
        original.setEnv("prod");
        original.setBizDomain("iot");
        ContextHolder.set(original);

        TraceContext captured = ContextHolder.capture();
        assertThat(captured).isNotNull();
        assertThat(captured.getTraceId()).isEqualTo("trace-1");
        assertThat(captured.getServiceName()).isEqualTo("svc-a");

        ContextHolder.remove();
        assertThat(ContextHolder.isSet()).isFalse();

        ContextHolder.restore(captured);
        assertThat(ContextHolder.getTraceId()).isEqualTo("trace-1");
        assertThat(ContextHolder.get().getServiceName()).isEqualTo("svc-a");
    }

    @Test
    @DisplayName("Should return null when capturing no context")
    void shouldReturnNullWhenCapturingNoContext() {
        TraceContext captured = ContextHolder.capture();
        assertThat(captured).isNull();
    }

    @Test
    @DisplayName("Should set null context without error")
    void shouldSetNullContext() {
        ContextHolder.set(null);
        assertThat(ContextHolder.isSet()).isFalse();
    }

    @Test
    @DisplayName("Should populate MDC fields when setting context")
    void shouldPopulateMdcFields() {
        TraceContext ctx = new TraceContext("trace-1", "span-1", "svc");
        ctx.setParentSpanId("parent-1");
        ctx.setEnv("prod");
        ctx.setBizDomain("iot");
        ctx.setTeamName("backend");
        ContextHolder.set(ctx);

        assertThat(org.slf4j.MDC.get("traceId")).isEqualTo("trace-1");
        assertThat(org.slf4j.MDC.get("spanId")).isEqualTo("span-1");
        assertThat(org.slf4j.MDC.get("parentSpanId")).isEqualTo("parent-1");
        assertThat(org.slf4j.MDC.get("serviceName")).isEqualTo("svc");
        assertThat(org.slf4j.MDC.get("env")).isEqualTo("prod");

        ContextHolder.remove();
        assertThat(org.slf4j.MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("Should handle context with null fields")
    void shouldHandleContextWithNullFields() {
        TraceContext ctx = new TraceContext();
        ctx.setTraceId("trace-1");
        ctx.setSpanId("span-1");
        // parentSpanId, env, etc. are null
        ContextHolder.set(ctx);

        assertThat(org.slf4j.MDC.get("traceId")).isEqualTo("trace-1");
        assertThat(org.slf4j.MDC.get("parentSpanId")).isEqualTo("");

        ContextHolder.remove();
    }
}
