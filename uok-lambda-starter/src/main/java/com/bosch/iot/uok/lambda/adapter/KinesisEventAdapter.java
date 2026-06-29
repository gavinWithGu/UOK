package com.bosch.iot.uok.lambda.adapter;

import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Kinesis event adapter for trace context extraction.
 * Extracts W3C Trace Context from Kinesis record headers/metadata.
 * <p>
 * In Kinesis, trace context can be passed through:
 * 1. Record headers (partition key or explicit headers)
 * 2. Embedded in record data (JSON with traceparent field)
 */
public class KinesisEventAdapter {

    private static final String TRACE_PARENT_KEY = "traceparent";
    private static final String TRACE_ID_KEY = "traceId";

    private KinesisEventAdapter() {
    }

    /**
     * Check if the event is a Kinesis event.
     *
     * @param event the event object
     * @return true if this is a Kinesis event
     */
    public static boolean isKinesisEvent(Object event) {
        if (event == null) {
            return false;
        }
        String className = event.getClass().getName();
        return className.contains("KinesisEvent") || className.contains("KinesisFirehoseEvent");
    }

    /**
     * Extract trace context from a Kinesis event.
     *
     * @param event       the Kinesis event
     * @param serviceName the service name
     * @return extracted TraceContext, or null if no context found
     */
    public static TraceContext extractTraceContext(Object event, String serviceName) {
        if (event == null) {
            return null;
        }

        // For KinesisEvent, iterate records and extract from first record
        // This is a simplified implementation - production would use reflection
        // to access the actual KinesisEvent.Records

        // Try to extract from event's toString or properties
        // In real implementation, would use:
        // KinesisEvent kinesisEvent = (KinesisEvent) event;
        // for (KinesisEventRecord record : kinesisEvent.getRecords()) {
        //     Map<String, String> headers = record.getKinesis().getHeaders();
        //     ...
        // }

        return null;
    }

    /**
     * Extract trace context from Kinesis record headers map.
     * Utility method that can be called from the actual Lambda handler.
     *
     * @param headers     the record headers
     * @param serviceName the service name
     * @return extracted TraceContext, or null if no context found
     */
    public static TraceContext extractFromHeaders(Map<String, String> headers, String serviceName) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        // Try W3C traceparent first
        String traceParent = headers.get(TRACE_PARENT_KEY);
        if (traceParent != null && !traceParent.isEmpty()) {
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
        String traceId = headers.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            TraceContext context = new TraceContext();
            context.setTraceId(traceId);
            context.setSpanId(TraceIdGenerator.generateSpanId());
            context.setServiceName(serviceName);
            context.setSampled(true);
            return context;
        }

        return null;
    }

    /**
     * Extract trace context from Kinesis record data (ByteBuffer).
     *
     * @param data        the record data buffer
     * @param serviceName the service name
     * @return extracted TraceContext, or null if no context found
     */
    public static TraceContext extractFromData(ByteBuffer data, String serviceName) {
        if (data == null) {
            return null;
        }

        try {
            byte[] bytes = new byte[data.remaining()];
            data.duplicate().get(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);

            // Simple extraction - look for traceparent in JSON
            if (json.contains(TRACE_PARENT_KEY)) {
                String traceParent = extractJsonField(json, TRACE_PARENT_KEY);
                if (traceParent != null) {
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
            }

            // Try direct traceId in JSON
            String traceId = extractJsonField(json, TRACE_ID_KEY);
            if (traceId != null && !traceId.isEmpty()) {
                TraceContext context = new TraceContext();
                context.setTraceId(traceId);
                context.setSpanId(TraceIdGenerator.generateSpanId());
                context.setServiceName(serviceName);
                context.setSampled(true);
                return context;
            }
        } catch (Exception e) {
            // Failed to parse data, return null
        }

        return null;
    }

    /**
     * Simple JSON field extraction (no dependency on JSON parser).
     */
    private static String extractJsonField(String json, String fieldName) {
        String pattern = "\"" + fieldName + "\"";
        int idx = json.indexOf(pattern);
        if (idx < 0) {
            return null;
        }
        int colonIdx = json.indexOf(':', idx + pattern.length());
        if (colonIdx < 0) {
            return null;
        }
        int startQuote = json.indexOf('"', colonIdx + 1);
        if (startQuote < 0) {
            return null;
        }
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        return json.substring(startQuote + 1, endQuote);
    }
}
