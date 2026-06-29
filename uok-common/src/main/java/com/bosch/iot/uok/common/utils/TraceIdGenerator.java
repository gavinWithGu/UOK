package com.bosch.iot.uok.common.utils;

import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Trace ID and Span ID generator.
 * Generates unique identifiers conforming to W3C Trace Context specification.
 * <p>
 * TraceID: 128-bit (32 hex characters), non-zero
 * SpanID: 64-bit (16 hex characters), non-zero
 */
public class TraceIdGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();

    /**
     * Generate a new 128-bit trace ID (32 hex characters).
     * Conforms to W3C Trace Context specification: must be non-zero.
     *
     * @return a new trace ID string
     */
    public static String generateTraceId() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        // Ensure non-zero: set at least one bit
        bytes[0] = (byte) (bytes[0] | 0x01);
        return HEX_FORMAT.formatHex(bytes);
    }

    /**
     * Generate a new 64-bit span ID (16 hex characters).
     * Conforms to W3C Trace Context specification: must be non-zero.
     *
     * @return a new span ID string
     */
    public static String generateSpanId() {
        byte[] bytes = new byte[8];
        SECURE_RANDOM.nextBytes(bytes);
        // Ensure non-zero: set at least one bit
        bytes[0] = (byte) (bytes[0] | 0x01);
        return HEX_FORMAT.formatHex(bytes);
    }

    /**
     * Validate a trace ID format.
     * Must be exactly 32 hex characters (128-bit) and non-zero.
     *
     * @param traceId the trace ID to validate
     * @return true if the trace ID is valid
     */
    public static boolean isValidTraceId(String traceId) {
        if (traceId == null || traceId.length() != 32) {
            return false;
        }
        // Check all hex characters and not all zeros
        boolean allZeros = true;
        for (int i = 0; i < traceId.length(); i++) {
            char c = traceId.charAt(i);
            if (!isHexChar(c)) {
                return false;
            }
            if (c != '0') {
                allZeros = false;
            }
        }
        return !allZeros;
    }

    /**
     * Validate a span ID format.
     * Must be exactly 16 hex characters (64-bit) and non-zero.
     *
     * @param spanId the span ID to validate
     * @return true if the span ID is valid
     */
    public static boolean isValidSpanId(String spanId) {
        if (spanId == null || spanId.length() != 16) {
            return false;
        }
        boolean allZeros = true;
        for (int i = 0; i < spanId.length(); i++) {
            char c = spanId.charAt(i);
            if (!isHexChar(c)) {
                return false;
            }
            if (c != '0') {
                allZeros = false;
            }
        }
        return !allZeros;
    }

    /**
     * Check if a character is a valid hex digit.
     */
    private static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
    }

    /**
     * Generate a W3C traceparent header value.
     * Format: version-traceId-spanId-flags
     *
     * @param traceId the trace ID
     * @param spanId  the span ID
     * @param sampled whether the trace is sampled
     * @return the traceparent header value
     */
    public static String formatTraceParent(String traceId, String spanId, boolean sampled) {
        String flags = sampled ? "01" : "00";
        return "00-" + traceId + "-" + spanId + "-" + flags;
    }

    /**
     * Parse a W3C traceparent header value.
     *
     * @param traceParent the traceparent header value
     * @return an array of [version, traceId, spanId, flags], or null if invalid
     */
    public static String[] parseTraceParent(String traceParent) {
        if (traceParent == null || traceParent.isEmpty()) {
            return null;
        }
        String[] parts = traceParent.split("-");
        if (parts.length != 4) {
            return null;
        }
        String version = parts[0];
        String traceId = parts[1];
        String spanId = parts[2];
        String flags = parts[3];

        if (!"00".equals(version)) {
            return null;
        }
        if (!isValidTraceId(traceId)) {
            return null;
        }
        if (!isValidSpanId(spanId)) {
            return null;
        }
        return new String[]{version, traceId, spanId, flags};
    }
}
