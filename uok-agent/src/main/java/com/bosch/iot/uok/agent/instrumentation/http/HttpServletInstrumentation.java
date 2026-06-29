package com.bosch.iot.uok.agent.instrumentation.http;

import com.bosch.iot.uok.agent.context.AgentContextHolder;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP Servlet instrumentation for trace context extraction and injection.
 * <p>
 * This class provides the core logic for:
 * 1. Extracting W3C Trace Context from incoming HTTP request headers
 * 2. Creating a root trace context if none exists
 * 3. Injecting trace context into MDC for log correlation
 * 4. Cleaning up context after request processing
 * <p>
 * In production with OTel Java Agent, this is handled by bytecode enhancement.
 * This implementation works as a servlet filter or direct call.
 */
public class HttpServletInstrumentation {

    private HttpServletInstrumentation() {
    }

    /**
     * Handle incoming HTTP request - extract or create trace context.
     *
     * @param traceParentHeader the traceparent header value (may be null)
     * @param headers           all request headers for business field extraction
     * @param serviceName       the current service name
     * @return the established TraceContext
     */
    public static TraceContext onHttpRequest(String traceParentHeader,
                                              Map<String, String> headers,
                                              String serviceName) {
        TraceContext context = null;

        // Try to extract from W3C traceparent header
        if (traceParentHeader != null && !traceParentHeader.isEmpty()) {
            context = AgentContextHolder.fromTraceParent(traceParentHeader, serviceName);
            if (context != null) {
                // Create a new child span for this service
                context = TraceContext.createChild(context,
                        TraceIdGenerator.generateSpanId(), serviceName);
            }
        }

        // If no valid traceparent, create root context
        if (context == null) {
            context = AgentContextHolder.createRootContext(serviceName);
        }

        // Set context in ThreadLocal holder
        ContextHolder.set(context);

        // Inject into MDC for log correlation
        MdcLogInjector.injectTraceContext(context);

        // Inject business fields from headers
        MdcLogInjector.injectBusinessFields(headers);

        return context;
    }

    /**
     * Handle outgoing HTTP request - inject trace context into request headers.
     *
     * @param headers the outgoing request headers map to inject into
     */
    public static void onHttpResponse(Map<String, String> headers) {
        TraceContext context = ContextHolder.get();
        if (context == null || headers == null) {
            return;
        }

        // Inject W3C traceparent header
        String traceParent = TraceIdGenerator.formatTraceParent(
                context.getTraceId(), context.getSpanId(), context.isSampled());
        headers.put(LogConstants.TRACE_PARENT_HEADER, traceParent);
    }

    /**
     * Clean up trace context after request processing.
     */
    public static void onHttpRequestComplete() {
        MdcLogInjector.clearTraceContext();
        ContextHolder.remove();
    }

    /**
     * Convert servlet request headers to a Map.
     *
     * @param headerNames  enumeration of header names
     * @param headerGetter function to get header value by name
     * @return map of header names to values
     */
    public static Map<String, String> extractHeaders(Enumeration<String> headerNames,
                                                      java.util.function.Function<String, String> headerGetter) {
        Map<String, String> headers = new HashMap<>();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, headerGetter.apply(name));
            }
        }
        return headers;
    }
}
