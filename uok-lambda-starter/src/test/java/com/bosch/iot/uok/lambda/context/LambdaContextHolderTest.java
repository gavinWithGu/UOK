package com.bosch.iot.uok.lambda.context;

import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LambdaContextHolder}.
 */
class LambdaContextHolderTest {

    @BeforeEach
    void setUp() {
        LambdaContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        LambdaContextHolder.clear();
    }

    @Test
    @DisplayName("Should return null when no context set")
    void shouldReturnNullWhenNoContext() {
        assertThat(LambdaContextHolder.get()).isNull();
        assertThat(LambdaContextHolder.getTraceId()).isNull();
        assertThat(LambdaContextHolder.getSpanId()).isNull();
        assertThat(LambdaContextHolder.isSet()).isFalse();
    }

    @Test
    @DisplayName("Should set and get trace context")
    void shouldSetAndGetContext() {
        TraceContext context = new TraceContext();
        context.setTraceId("test-trace-id");
        context.setSpanId("test-span-id");
        context.setServiceName("test-service");

        LambdaContextHolder.set(context);

        assertThat(LambdaContextHolder.get()).isSameAs(context);
        assertThat(LambdaContextHolder.isSet()).isTrue();
    }

    @Test
    @DisplayName("Should get trace ID from context")
    void shouldGetTraceId() {
        TraceContext context = new TraceContext();
        context.setTraceId("abc123def456");
        context.setSpanId("span789");

        LambdaContextHolder.set(context);

        assertThat(LambdaContextHolder.getTraceId()).isEqualTo("abc123def456");
    }

    @Test
    @DisplayName("Should get span ID from context")
    void shouldGetSpanId() {
        TraceContext context = new TraceContext();
        context.setTraceId("trace123");
        context.setSpanId("span456");

        LambdaContextHolder.set(context);

        assertThat(LambdaContextHolder.getSpanId()).isEqualTo("span456");
    }

    @Test
    @DisplayName("Should clear context")
    void shouldClearContext() {
        TraceContext context = new TraceContext();
        context.setTraceId("trace-id");
        context.setSpanId("span-id");

        LambdaContextHolder.set(context);
        assertThat(LambdaContextHolder.isSet()).isTrue();

        LambdaContextHolder.clear();
        assertThat(LambdaContextHolder.isSet()).isFalse();
        assertThat(LambdaContextHolder.get()).isNull();
        assertThat(LambdaContextHolder.getTraceId()).isNull();
        assertThat(LambdaContextHolder.getSpanId()).isNull();
    }

    @Test
    @DisplayName("Should overwrite context on new set")
    void shouldOverwriteContextOnNewSet() {
        TraceContext ctx1 = new TraceContext();
        ctx1.setTraceId("trace-1");
        ctx1.setSpanId("span-1");

        LambdaContextHolder.set(ctx1);
        assertThat(LambdaContextHolder.getTraceId()).isEqualTo("trace-1");

        TraceContext ctx2 = new TraceContext();
        ctx2.setTraceId("trace-2");
        ctx2.setSpanId("span-2");

        LambdaContextHolder.set(ctx2);
        assertThat(LambdaContextHolder.getTraceId()).isEqualTo("trace-2");
        assertThat(LambdaContextHolder.getSpanId()).isEqualTo("span-2");
    }

    @Test
    @DisplayName("Should handle null trace ID in context")
    void shouldHandleNullTraceId() {
        TraceContext context = new TraceContext();
        context.setTraceId(null);
        context.setSpanId("span-id");

        LambdaContextHolder.set(context);

        assertThat(LambdaContextHolder.getTraceId()).isNull();
        assertThat(LambdaContextHolder.getSpanId()).isEqualTo("span-id");
    }

    @Test
    @DisplayName("Should handle null span ID in context")
    void shouldHandleNullSpanId() {
        TraceContext context = new TraceContext();
        context.setTraceId("trace-id");
        context.setSpanId(null);

        LambdaContextHolder.set(context);

        assertThat(LambdaContextHolder.getTraceId()).isEqualTo("trace-id");
        assertThat(LambdaContextHolder.getSpanId()).isNull();
    }

    @Test
    @DisplayName("Clear should be idempotent")
    void clearShouldBeIdempotent() {
        LambdaContextHolder.clear();
        LambdaContextHolder.clear();
        assertThat(LambdaContextHolder.isSet()).isFalse();
    }
}
