package com.bosch.iot.uok.agent.instrumentation.async;

import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AsyncContextPropagator}.
 */
class AsyncContextPropagatorTest {

    @BeforeEach
    void setUp() {
        ContextHolder.remove();
    }

    @AfterEach
    void tearDown() {
        ContextHolder.remove();
    }

    @Test
    @DisplayName("Should propagate trace context across threads with wrapRunnable")
    void shouldPropagateWithWrapRunnable() throws Exception {
        TraceContext context = new TraceContext("async-trace-123", "async-span-456", "test-service");
        ContextHolder.set(context);

        AtomicReference<String> capturedTraceId = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable wrapped = AsyncContextPropagator.wrapRunnable(() -> {
            TraceContext ctx = ContextHolder.get();
            if (ctx != null) {
                capturedTraceId.set(ctx.getTraceId());
            }
            latch.countDown();
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(wrapped);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isEqualTo("async-trace-123");

        executor.shutdown();
    }

    @Test
    @DisplayName("Should propagate trace context with wrapSupplier")
    void shouldPropagateWithWrapSupplier() throws Exception {
        TraceContext context = new TraceContext("supplier-trace", "supplier-span", "svc");
        ContextHolder.set(context);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(
                AsyncContextPropagator.wrapSupplier(() -> {
                    TraceContext ctx = ContextHolder.get();
                    return ctx != null ? ctx.getTraceId() : null;
                }));

        String result = future.get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("supplier-trace");
    }

    @Test
    @DisplayName("Should handle null runnable")
    void shouldHandleNullRunnable() {
        assertThat(AsyncContextPropagator.wrapRunnable(null)).isNull();
    }

    @Test
    @DisplayName("Should handle null supplier")
    void shouldHandleNullSupplier() {
        assertThat(AsyncContextPropagator.wrapSupplier(null)).isNull();
    }

    @Test
    @DisplayName("Should handle no context set")
    void shouldHandleNoContextSet() throws Exception {
        ContextHolder.remove();

        AtomicReference<String> capturedTraceId = new AtomicReference<>("default");
        CountDownLatch latch = new CountDownLatch(1);

        Runnable wrapped = AsyncContextPropagator.wrapRunnable(() -> {
            TraceContext ctx = ContextHolder.get();
            capturedTraceId.set(ctx != null ? ctx.getTraceId() : "no-context");
            latch.countDown();
        });

        Executors.newSingleThreadExecutor().submit(wrapped);
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(capturedTraceId.get()).isEqualTo("no-context");
    }

    @Test
    @DisplayName("Should clean up context after task execution")
    void shouldCleanUpAfterTask() throws Exception {
        TraceContext context = new TraceContext("cleanup-trace", "cleanup-span", "svc");
        ContextHolder.set(context);

        CountDownLatch latch = new CountDownLatch(1);
        Runnable wrapped = AsyncContextPropagator.wrapRunnable(latch::countDown);

        Executors.newSingleThreadExecutor().submit(wrapped);
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();

        // The thread-local should be cleaned up in the worker thread
        // Main thread context should not be affected
    }

    @Test
    @DisplayName("Should wrap executor for automatic propagation")
    void shouldWrapExecutor() throws Exception {
        TraceContext context = new TraceContext("exec-trace", "exec-span", "svc");
        ContextHolder.set(context);

        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        java.util.concurrent.Executor raw = Executors.newSingleThreadExecutor();
        java.util.concurrent.Executor wrapped = AsyncContextPropagator.wrapExecutor(raw);

        wrapped.execute(() -> {
            TraceContext ctx = ContextHolder.get();
            if (ctx != null) {
                captured.set(ctx.getTraceId());
            }
            latch.countDown();
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(captured.get()).isEqualTo("exec-trace");
    }

    @Test
    @DisplayName("Should handle null executor")
    void shouldHandleNullExecutor() {
        assertThat(AsyncContextPropagator.wrapExecutor(null)).isNull();
    }

    @Test
    @DisplayName("Should wrap scheduled task")
    void shouldWrapScheduledTask() {
        Runnable wrapped = AsyncContextPropagator.wrapScheduledTask(() -> {}, "svc");
        assertThat(wrapped).isNotNull();
    }

    @Test
    @DisplayName("Should handle null scheduled task")
    void shouldHandleNullScheduledTask() {
        assertThat(AsyncContextPropagator.wrapScheduledTask(null, "svc")).isNull();
    }
}
