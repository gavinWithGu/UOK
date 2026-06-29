package com.bosch.iot.uok.lambda.adapter;

import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;

import java.util.Map;

/**
 * Kafka/MSK event adapter for trace context extraction.
 * Extracts W3C Trace Context from Kafka event headers.
 * <p>
 * For Lambda functions triggered by MSK (AWS Managed Kafka),
 * trace context is passed through Kafka message headers.
 */
public class KafkaEventAdapter {

    private static final String TRACE_PARENT_KEY = "traceparent";
    private static final String TRACE_ID_KEY = "traceId";

    private KafkaEventAdapter() {
    }

    /**
     * Check if the event is a Kafka event.
     *
     * @param event the event object
     * @return true if this is a Kafka event
     */
    public static boolean isKafkaEvent(Object event) {
        if (event == null) {
            return false;
        }
        String className = event.getClass().getName();
        return className.contains("KafkaEvent") || className.contains("MSKEvent")
                || className.contains("KafkaRecord");
    }

    /**
     * Extract trace context from a Kafka event.
     *
     * @param event       the Kafka event
     * @param serviceName the service name
     * @return extracted TraceContext, or null if no context found
     */
    public static TraceContext extractTraceContext(Object event, String serviceName) {
        // In production, would use reflection to access KafkaEvent records
        // For now, returns null - actual extraction done via extractFromHeaders
        return null;
    }

    /**
     * Extract trace context from Kafka record headers map.
     * Utility method called from the actual Lambda handler.
     *
     * @param headers     the record headers
     * @param serviceName the service name
     * @return extracted TraceContext, or null if no context found
     */
    public static TraceContext extractFromHeaders(Map<String, byte[]> headers, String serviceName) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        // Try W3C traceparent header
        byte[] traceParentBytes = headers.get(TRACE_PARENT_KEY);
        if (traceParentBytes != null) {
            String traceParent = new String(traceParentBytes, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = TraceIdGenerator.parseTraceParent(traceParent);
            if (parts != null) {
                TraceContext context = new TraceContext();
                context.setTraceId(parts[1]);
                context.setSpanId(TraceIdGenerator.generateSpanId());
                context.setParentSpanId(parts[2]);
                context.setServiceName(serviceName);
                context.setSampled("01".equals(parts[3]));
                return context;
            }
        }

        // Try direct traceId header
        byte[] traceIdBytes = headers.get(TRACE_ID_KEY);
        if (traceIdBytes != null) {
            String traceId = new String(traceIdBytes, java.nio.charset.StandardCharsets.UTF_8);
            if (traceId != null && !traceId.isEmpty()) {
                TraceContext context = new TraceContext();
                context.setTraceId(traceId);
                context.setSpanId(TraceIdGenerator.generateSpanId());
                context.setServiceName(serviceName);
                context.setSampled(true);
                return context;
            }
        }

        return null;
    }
}
