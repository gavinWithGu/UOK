package com.bosch.iot.uok.agent.instrumentation.kafka;

import com.bosch.iot.uok.agent.context.AgentContextHolder;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kafka instrumentation for trace context propagation via message headers.
 * <p>
 * Producer: injects trace context into Kafka message headers
 * Consumer: extracts trace context from Kafka message headers
 * <p>
 * Works with both Kafka native clients and AWS MSK.
 * Does not modify message payload - only adds/reads headers.
 */
public class KafkaInstrumentation {

    private KafkaInstrumentation() {
    }

    /**
     * Inject trace context into Kafka producer record headers.
     * Called before sending a message to Kafka.
     *
     * @param headers the Kafka headers map to inject into
     */
    public static void onProduce(Map<String, byte[]> headers) {
        if (headers == null) {
            return;
        }

        TraceContext context = ContextHolder.get();
        if (context == null) {
            // Create root context if none exists
            context = AgentContextHolder.createRootContext(
                    System.getProperty("uok.service.name", "unknown-service"));
            ContextHolder.set(context);
        }

        // Inject trace fields into Kafka headers
        headers.put(LogConstants.KAFKA_TRACE_ID_KEY,
                context.getTraceId().getBytes(StandardCharsets.UTF_8));
        headers.put(LogConstants.KAFKA_SPAN_ID_KEY,
                context.getSpanId().getBytes(StandardCharsets.UTF_8));
        if (context.getParentSpanId() != null) {
            headers.put(LogConstants.KAFKA_PARENT_SPAN_ID_KEY,
                    context.getParentSpanId().getBytes(StandardCharsets.UTF_8));
        }
        headers.put(LogConstants.KAFKA_SAMPLED_KEY,
                String.valueOf(context.isSampled()).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extract trace context from Kafka consumer record headers.
     * Called after receiving a message from Kafka.
     *
     * @param headers      the Kafka headers map
     * @param serviceName  the consuming service name
     * @return the extracted TraceContext, or a new root context if none found
     */
    public static TraceContext onConsume(Map<String, byte[]> headers, String serviceName) {
        TraceContext context = null;

        if (headers != null) {
            String traceId = getHeaderString(headers, LogConstants.KAFKA_TRACE_ID_KEY);
            String parentSpanId = getHeaderString(headers, LogConstants.KAFKA_SPAN_ID_KEY);
            boolean sampled = Boolean.parseBoolean(
                    getHeaderString(headers, LogConstants.KAFKA_SAMPLED_KEY, "true"));

            if (traceId != null && !traceId.isEmpty()) {
                context = AgentContextHolder.createChildContext(traceId, parentSpanId, serviceName);
                context.setSampled(sampled);
            }
        }

        // If no trace context found in headers, create root
        if (context == null) {
            context = AgentContextHolder.createRootContext(serviceName);
        }

        // Set context in ThreadLocal holder
        ContextHolder.set(context);

        // Inject into MDC
        MdcLogInjector.injectTraceContext(context);

        return context;
    }

    /**
     * Clean up trace context after message processing.
     */
    public static void onConsumeComplete() {
        MdcLogInjector.clearTraceContext();
        ContextHolder.remove();
    }

    private static String getHeaderString(Map<String, byte[]> headers, String key) {
        return getHeaderString(headers, key, null);
    }

    private static String getHeaderString(Map<String, byte[]> headers, String key, String defaultValue) {
        byte[] value = headers.get(key);
        if (value == null) {
            return defaultValue;
        }
        return new String(value, StandardCharsets.UTF_8);
    }
}
