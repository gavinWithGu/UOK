package com.bosch.iot.uok.agent.context;

import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;

/**
 * Agent-specific context holder that bridges OTel Span context
 * with UOK TraceContext model.
 * <p>
 * Provides bidirectional conversion between:
 * - OTel Span/SpanContext ↔ UOK TraceContext
 * - OTel Context propagation ↔ UOK ContextHolder
 */
public class AgentContextHolder {

    private AgentContextHolder() {
    }

    /**
     * Create a UOK TraceContext from an OTel Span.
     *
     * @param span        the OTel span
     * @param serviceName the service name
     * @return a UOK TraceContext
     */
    public static TraceContext fromOtelSpan(Span span, String serviceName) {
        if (span == null || !span.getSpanContext().isValid()) {
            return null;
        }

        SpanContext spanContext = span.getSpanContext();
        TraceContext traceContext = new TraceContext();
        traceContext.setTraceId(spanContext.getTraceId());
        traceContext.setSpanId(spanContext.getSpanId());
        traceContext.setServiceName(serviceName);
        traceContext.setSampled(spanContext.isSampled());

        return traceContext;
    }

    /**
     * Create a root TraceContext with newly generated IDs.
     *
     * @param serviceName the service name
     * @return a new root TraceContext
     */
    public static TraceContext createRootContext(String serviceName) {
        TraceContext context = new TraceContext();
        context.setTraceId(TraceIdGenerator.generateTraceId());
        context.setSpanId(TraceIdGenerator.generateSpanId());
        context.setServiceName(serviceName);
        context.setSampled(true);
        return context;
    }

    /**
     * Create a child TraceContext from a parent context.
     *
     * @param parentTraceId the parent trace ID
     * @param parentSpanId  the parent span ID
     * @param serviceName   the child service name
     * @return a new child TraceContext
     */
    public static TraceContext createChildContext(String parentTraceId, String parentSpanId,
                                                   String serviceName) {
        TraceContext context = new TraceContext();
        context.setTraceId(parentTraceId);
        context.setSpanId(TraceIdGenerator.generateSpanId());
        context.setParentSpanId(parentSpanId);
        context.setServiceName(serviceName);
        context.setSampled(true);
        return context;
    }

    /**
     * Extract trace context from W3C traceparent header.
     *
     * @param traceParent the traceparent header value
     * @param serviceName the service name
     * @return a TraceContext, or null if the traceparent is invalid
     */
    public static TraceContext fromTraceParent(String traceParent, String serviceName) {
        String[] parts = TraceIdGenerator.parseTraceParent(traceParent);
        if (parts == null) {
            return null;
        }

        TraceContext context = new TraceContext();
        context.setTraceId(parts[1]);
        context.setSpanId(parts[2]);
        context.setServiceName(serviceName);
        context.setSampled("01".equals(parts[3]));
        return context;
    }
}
