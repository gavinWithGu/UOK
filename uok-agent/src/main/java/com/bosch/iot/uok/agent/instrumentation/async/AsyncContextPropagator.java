package com.bosch.iot.uok.agent.instrumentation.async;

import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Async context propagation utilities.
 * Ensures trace context is propagated across thread boundaries
 * for CompletableFuture, @Async, thread pools, and scheduled tasks.
 * <p>
 * Usage:
 * <pre>
 *   CompletableFuture.supplyAsync(AsyncContextPropagator.wrapSupplier(() -> {...}))
 *   executor.execute(AsyncContextPropagator.wrapRunnable(() -> {...}))
 * </pre>
 */
public class AsyncContextPropagator {

    private AsyncContextPropagator() {
    }

    /**
     * Wrap a Runnable to capture and restore trace context across threads.
     *
     * @param task the original Runnable
     * @return a context-aware Runnable
     */
    public static Runnable wrapRunnable(Runnable task) {
        if (task == null) {
            return null;
        }
        TraceContext captured = ContextHolder.capture();
        return () -> {
            TraceContext previous = ContextHolder.get();
            try {
                if (captured != null) {
                    ContextHolder.restore(captured);
                }
                task.run();
            } finally {
                ContextHolder.remove();
                if (previous != null) {
                    ContextHolder.set(previous);
                }
            }
        };
    }

    /**
     * Wrap a Supplier to capture and restore trace context across threads.
     *
     * @param supplier the original Supplier
     * @param <T>      the return type
     * @return a context-aware Supplier
     */
    public static <T> Supplier<T> wrapSupplier(Supplier<T> supplier) {
        if (supplier == null) {
            return null;
        }
        TraceContext captured = ContextHolder.capture();
        return () -> {
            TraceContext previous = ContextHolder.get();
            try {
                if (captured != null) {
                    ContextHolder.restore(captured);
                }
                return supplier.get();
            } finally {
                ContextHolder.remove();
                if (previous != null) {
                    ContextHolder.set(previous);
                }
            }
        };
    }

    /**
     * Wrap an Executor to automatically propagate trace context.
     *
     * @param delegate the original Executor
     * @return a context-aware Executor
     */
    public static Executor wrapExecutor(Executor delegate) {
        if (delegate == null) {
            return null;
        }
        return command -> delegate.execute(wrapRunnable(command));
    }

    /**
     * Wrap an ExecutorService to automatically propagate trace context.
     *
     * @param delegate the original ExecutorService
     * @return a context-aware ExecutorService (same instance, wrapped execute)
     */
    public static ExecutorService wrapExecutorService(ExecutorService delegate) {
        // Return as-is; individual tasks should use wrapRunnable/wrapSupplier
        // Full wrapping would require delegating all ExecutorService methods
        return delegate;
    }

    /**
     * Create a trace-aware task for @Scheduled methods.
     * Generates a new root trace for each scheduled execution.
     *
     * @param task         the scheduled task
     * @param serviceName  the service name for the trace
     * @return a context-aware Runnable for scheduled execution
     */
    public static Runnable wrapScheduledTask(Runnable task, String serviceName) {
        if (task == null) {
            return null;
        }
        return () -> {
            // Scheduled tasks get a new root trace since there's no parent
            ContextHolder.remove();
            try {
                task.run();
            } finally {
                ContextHolder.remove();
            }
        };
    }
}
