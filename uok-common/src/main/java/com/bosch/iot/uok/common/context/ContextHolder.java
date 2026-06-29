package com.bosch.iot.uok.common.context;

import org.slf4j.MDC;

/**
 * Thread-local holder for trace context.
 * Manages the current trace context in a thread-local variable
 * and synchronizes with SLF4J MDC for automatic log injection.
 */
public class ContextHolder {

    private static final ThreadLocal<TraceContext> CONTEXT = new ThreadLocal<>();

    private ContextHolder() {
        // Prevent instantiation
    }

    /**
     * Set the current trace context for this thread.
     * Also populates SLF4J MDC with trace fields for automatic log injection.
     *
     * @param context the trace context to set
     */
    public static void set(TraceContext context) {
        CONTEXT.set(context);
        if (context != null) {
            MDC.put("traceId", context.getTraceId() != null ? context.getTraceId() : "");
            MDC.put("spanId", context.getSpanId() != null ? context.getSpanId() : "");
            MDC.put("parentSpanId", context.getParentSpanId() != null ? context.getParentSpanId() : "");
            MDC.put("serviceName", context.getServiceName() != null ? context.getServiceName() : "");
            MDC.put("env", context.getEnv() != null ? context.getEnv() : "");
            MDC.put("bizDomain", context.getBizDomain() != null ? context.getBizDomain() : "");
            MDC.put("teamName", context.getTeamName() != null ? context.getTeamName() : "");
        }
    }

    /**
     * Get the current trace context for this thread.
     *
     * @return the current trace context, or null if not set
     */
    public static TraceContext get() {
        return CONTEXT.get();
    }

    /**
     * Remove the current trace context for this thread.
     * Also clears SLF4J MDC fields.
     */
    public static void remove() {
        CONTEXT.remove();
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("parentSpanId");
        MDC.remove("serviceName");
        MDC.remove("env");
        MDC.remove("bizDomain");
        MDC.remove("teamName");
    }

    /**
     * Check if a trace context is currently set for this thread.
     *
     * @return true if a trace context is set
     */
    public static boolean isSet() {
        return CONTEXT.get() != null;
    }

    /**
     * Get the current trace ID from the context.
     *
     * @return the current trace ID, or null if not set
     */
    public static String getTraceId() {
        TraceContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getTraceId() : null;
    }

    /**
     * Get the current span ID from the context.
     *
     * @return the current span ID, or null if not set
     */
    public static String getSpanId() {
        TraceContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getSpanId() : null;
    }

    /**
     * Get the current parent span ID from the context.
     *
     * @return the current parent span ID, or null if not set
     */
    public static String getParentSpanId() {
        TraceContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getParentSpanId() : null;
    }

    /**
     * Capture the current trace context for cross-thread propagation.
     *
     * @return a snapshot of the current trace context
     */
    public static TraceContext capture() {
        TraceContext current = CONTEXT.get();
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
     * Restore a previously captured trace context.
     *
     * @param captured the captured trace context to restore
     */
    public static void restore(TraceContext captured) {
        set(captured);
    }
}
