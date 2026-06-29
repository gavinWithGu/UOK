package com.bosch.iot.uok.common.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ReactiveContextHolder}.
 */
class ReactiveContextHolderTest {

    @BeforeEach
    void setUp() {
        ReactiveContextHolder.clear();
    }

    @AfterEach
    void tearDown() {
        ReactiveContextHolder.clear();
    }

    @Test
    @DisplayName("Should set and get reactive context")
    void shouldSetAndGetContext() {
        TraceContext ctx = new TraceContext("trace-1", "span-1", "svc");
        ReactiveContextHolder.set(ctx);

        assertThat(ReactiveContextHolder.get()).isNotNull();
        assertThat(ReactiveContextHolder.get().getTraceId()).isEqualTo("trace-1");
    }

    @Test
    @DisplayName("Should return null when no context is set")
    void shouldReturnNullWhenNotSet() {
        assertThat(ReactiveContextHolder.get()).isNull();
        assertThat(ReactiveContextHolder.isSet()).isFalse();
    }

    @Test
    @DisplayName("Should check if context is set")
    void shouldCheckIfSet() {
        assertThat(ReactiveContextHolder.isSet()).isFalse();

        ReactiveContextHolder.set(new TraceContext("t", "s", "svc"));
        assertThat(ReactiveContextHolder.isSet()).isTrue();
    }

    @Test
    @DisplayName("Should clear context")
    void shouldClearContext() {
        ReactiveContextHolder.set(new TraceContext("t", "s", "svc"));
        assertThat(ReactiveContextHolder.isSet()).isTrue();

        ReactiveContextHolder.clear();
        assertThat(ReactiveContextHolder.isSet()).isFalse();
        assertThat(ReactiveContextHolder.get()).isNull();
    }

    @Test
    @DisplayName("Should capture and restore context")
    void shouldCaptureAndRestoreContext() {
        TraceContext original = new TraceContext("trace-1", "span-1", "svc-a");
        original.setEnv("prod");
        ReactiveContextHolder.set(original);

        TraceContext captured = ReactiveContextHolder.capture();
        assertThat(captured).isNotNull();
        assertThat(captured.getTraceId()).isEqualTo("trace-1");

        ReactiveContextHolder.clear();
        assertThat(ReactiveContextHolder.isSet()).isFalse();

        ReactiveContextHolder.restore(captured);
        assertThat(ReactiveContextHolder.get()).isNotNull();
        assertThat(ReactiveContextHolder.get().getTraceId()).isEqualTo("trace-1");
    }

    @Test
    @DisplayName("Should return null when capturing no context")
    void shouldReturnNullWhenCapturingNoContext() {
        TraceContext captured = ReactiveContextHolder.capture();
        assertThat(captured).isNull();
    }

    @Test
    @DisplayName("Should return correct context key")
    void shouldReturnContextKey() {
        assertThat(ReactiveContextHolder.getContextKey()).isEqualTo("uok.trace.context");
    }
}
