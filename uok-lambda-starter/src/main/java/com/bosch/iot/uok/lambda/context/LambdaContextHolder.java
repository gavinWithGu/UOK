package com.bosch.iot.uok.lambda.context;

import com.bosch.iot.uok.common.context.TraceContext;

/**
 * Lambda-specific context holder.
 * Manages trace context within Lambda's short lifecycle.
 * <p>
 * Unlike thread-local based holders, Lambda functions typically
 * handle one event at a time, so a simple volatile reference suffices.
 */
public class LambdaContextHolder {

    private static volatile TraceContext currentContext;

    private LambdaContextHolder() {
    }

    /**
     * Set the current trace context.
     *
     * @param context the trace context
     */
    public static void set(TraceContext context) {
        currentContext = context;
    }

    /**
     * Get the current trace context.
     *
     * @return the current trace context, or null if not set
     */
    public static TraceContext get() {
        return currentContext;
    }

    /**
     * Get the current trace ID.
     *
     * @return the current trace ID, or null if not set
     */
    public static String getTraceId() {
        TraceContext ctx = currentContext;
        return ctx != null ? ctx.getTraceId() : null;
    }

    /**
     * Get the current span ID.
     *
     * @return the current span ID, or null if not set
     */
    public static String getSpanId() {
        TraceContext ctx = currentContext;
        return ctx != null ? ctx.getSpanId() : null;
    }

    /**
     * Clear the current context.
     */
    public static void clear() {
        currentContext = null;
    }

    /**
     * Check if a context is set.
     *
     * @return true if a context is set
     */
    public static boolean isSet() {
        return currentContext != null;
    }
}
