package com.bosch.iot.uok.agent.context;

import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AgentContextHolder}.
 */
class AgentContextHolderTest {

    @Test
    @DisplayName("Should create root context with valid IDs")
    void shouldCreateRootContext() {
        TraceContext context = AgentContextHolder.createRootContext("test-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isNotNull();
        assertThat(context.getTraceId().length()).isEqualTo(32);
        assertThat(context.getSpanId()).isNotNull();
        assertThat(context.getSpanId().length()).isEqualTo(16);
        assertThat(context.getServiceName()).isEqualTo("test-service");
        assertThat(context.isRoot()).isTrue();
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should create child context from parent")
    void shouldCreateChildContext() {
        TraceContext context = AgentContextHolder.createChildContext(
                "0123456789abcdef0123456789abcdef",
                "0123456789abcdef",
                "child-service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(context.getSpanId()).isNotNull();
        assertThat(context.getParentSpanId()).isEqualTo("0123456789abcdef");
        assertThat(context.getServiceName()).isEqualTo("child-service");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should extract context from valid traceparent")
    void shouldExtractFromValidTraceParent() {
        String traceParent = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";
        TraceContext context = AgentContextHolder.fromTraceParent(traceParent, "service");

        assertThat(context).isNotNull();
        assertThat(context.getTraceId()).isEqualTo("0123456789abcdef0123456789abcdef");
        assertThat(context.getSpanId()).isEqualTo("0123456789abcdef");
        assertThat(context.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should extract unsampled context from traceparent")
    void shouldExtractUnsampledFromTraceParent() {
        String traceParent = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-00";
        TraceContext context = AgentContextHolder.fromTraceParent(traceParent, "service");

        assertThat(context).isNotNull();
        assertThat(context.isSampled()).isFalse();
    }

    @Test
    @DisplayName("Should return null for invalid traceparent")
    void shouldReturnNullForInvalidTraceParent() {
        assertThat(AgentContextHolder.fromTraceParent(null, "service")).isNull();
        assertThat(AgentContextHolder.fromTraceParent("", "service")).isNull();
        assertThat(AgentContextHolder.fromTraceParent("invalid", "service")).isNull();
        assertThat(AgentContextHolder.fromTraceParent("01-abc-def-01", "service")).isNull();
    }

    @Test
    @DisplayName("Should return null for null span in fromOtelSpan")
    void shouldReturnNullForNullSpan() {
        assertThat(AgentContextHolder.fromOtelSpan(null, "service")).isNull();
    }
}
