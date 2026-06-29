package com.bosch.iot.uok.agent.instrumentation.webflux;

import com.bosch.iot.uok.agent.context.AgentContextHolder;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.context.ReactiveContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;

import java.util.Map;
import java.util.function.Function;

/**
 * WebFlux/Reactor instrumentation for trace context propagation.
 * <p>
 * Provides utilities to propagate trace context through reactive pipelines
 * using Reactor's Context API and UOK's ReactiveContextHolder.
 * <p>
 * Usage:
 * <pre>
 *   Mono.just(data)
 *       .flatMap(WebFluxInstrumentation.wrapFunction(data -> {...}))
 *       .contextWrite(context -> WebFluxInstrumentation.injectToReactorContext(context, traceContext))
 * </pre>
 */
public class WebFluxInstrumentation {

    private static final String REACTOR_CONTEXT_KEY = "uok.trace.context";

    private WebFluxInstrumentation() {
    }

    /**
     * Handle incoming WebFlux request - extract or create trace context.
     *
     * @param traceParentHeader the traceparent header value
     * @param headers           request headers
     * @param serviceName       the service name
     * @return the established TraceContext
     */
    public static TraceContext onWebFluxRequest(String traceParentHeader,
                                                 Map<String, String> headers,
                                                 String serviceName) {
        TraceContext context = null;

        if (traceParentHeader != null && !traceParentHeader.isEmpty()) {
            context = AgentContextHolder.fromTraceParent(traceParentHeader, serviceName);
            if (context != null) {
                context = TraceContext.createChild(context,
                        TraceIdGenerator.generateSpanId(), serviceName);
            }
        }

        if (context == null) {
            context = AgentContextHolder.createRootContext(serviceName);
        }

        // Store in reactive context holder
        ReactiveContextHolder.set(context);

        // Also inject into MDC for log correlation
        MdcLogInjector.injectTraceContext(context);

        return context;
    }

    /**
     * Clean up context after WebFlux request processing.
     */
    public static void onWebFluxRequestComplete() {
        MdcLogInjector.clearTraceContext();
        ReactiveContextHolder.clear();
    }

    /**
     * Wrap a Function for use in reactive pipelines with context propagation.
     *
     * @param fn  the original function
     * @param <T> input type
     * @param <R> output type
     * @return a context-aware Function
     */
    public static <T, R> Function<T, R> wrapFunction(Function<T, R> fn) {
        if (fn == null) {
            return null;
        }
        return input -> {
            // Restore context from reactive holder if available
            TraceContext captured = ReactiveContextHolder.capture();
            if (captured != null) {
                MdcLogInjector.injectTraceContext(captured);
            }
            try {
                return fn.apply(input);
            } finally {
                MdcLogInjector.clearTraceContext();
            }
        };
    }
}
