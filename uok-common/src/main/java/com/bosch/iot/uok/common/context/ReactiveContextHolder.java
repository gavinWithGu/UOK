package com.bosch.iot.uok.common.context;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reactive context holder for Spring WebFlux / Project Reactor integration.
 * Unlike thread-local based {@link ContextHolder}, this provides a way
 * to store and retrieve trace context within reactive pipelines.
 * <p>
 * This class is designed to work with Reactor's Context API.
 * Integration with Reactor Context is done via static helper methods.
 */
public class ReactiveContextHolder {

    private static final String TRACE_CONTEXT_KEY = "uok.trace.context";

    private static final AtomicReference<TraceContext> REACTIVE_CONTEXT = new AtomicReference<>();

    private ReactiveContextHolder() {
        // Prevent instantiation
    }

    /**
     * Get the Reactor context key for trace context.
     *
     * @return the context key string
     */
    public static String getContextKey() {
        return TRACE_CONTEXT_KEY;
    }

    /**
     * Set the trace context in the reactive context holder.
     *
     * @param context the trace context to set
     */
    public static void set(TraceContext context) {
        REACTIVE_CONTEXT.set(context);
    }

    /**
     * Get the trace context from the reactive context holder.
     *
     * @return the current trace context, or null if not set
     */
    public static TraceContext get() {
        return REACTIVE_CONTEXT.get();
    }

    /**
     * Clear the reactive context holder.
     */
    public static void clear() {
        REACTIVE_CONTEXT.set(null);
    }

    /**
     * Check if a reactive trace context is currently set.
     *
     * @return true if a trace context is set
     */
    public static boolean isSet() {
        return REACTIVE_CONTEXT.get() != null;
    }

    /**
     * Capture the current reactive trace context.
     *
     * @return a snapshot of the current trace context
     */
    public static TraceContext capture() {
        TraceContext current = REACTIVE_CONTEXT.get();
        if (current == null) {
            return null;
        }
        TraceContext snapshot = new TraceContext();
        snapshot.setTraceId(current.getTraceId());
        snapshot.setSpanId(current.getSpanId());
        snapshot.setParentSpanId(current.getParentSpanId());
        snapshot.setServiceName(current.getServiceName());
        snapshot.setEnv(current.getEnv());
        snapshot.setBizDomain(current.getBizDomain());
        snapshot.setTeamName(current.getTeamName());
        snapshot.setSampled(current.isSampled());
        return snapshot;
    }

    /**
     * Restore a previously captured trace context into the reactive holder.
     *
     * @param captured the captured trace context to restore
     */
    public static void restore(TraceContext captured) {
        REACTIVE_CONTEXT.set(captured);
    }
}
