package com.bosch.iot.uok.common.constant;

/**
 * Log field constants for structured logging.
 * All log fields injected by UOK into MDC and structured log output.
 */
public final class LogConstants {

    // --- Standard Trace Fields ---
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String PARENT_SPAN_ID = "parentSpanId";

    // --- Service Identity Fields ---
    public static final String SERVICE_NAME = "serviceName";
    public static final String ENV = "env";
    public static final String BIZ_DOMAIN = "bizDomain";
    public static final String TEAM_NAME = "teamName";

    // --- Standard Log Fields ---
    public static final String TIMESTAMP = "timestamp";
    public static final String LEVEL = "level";
    public static final String LOGGER = "logger";
    public static final String THREAD = "thread";
    public static final String MESSAGE = "message";

    // --- Business Extension Fields ---
    public static final String DEVICE_ID = "deviceId";
    public static final String USER_ID = "userId";
    public static final String ERROR_CODE = "errorCode";
    public static final String ORDER_ID = "orderId";

    // --- W3C Trace Context Header Names ---
    public static final String TRACE_PARENT_HEADER = "traceparent";
    public static final String TRACE_STATE_HEADER = "tracestate";

    // --- Kafka Header Keys ---
    public static final String KAFKA_TRACE_ID_KEY = "traceId";
    public static final String KAFKA_SPAN_ID_KEY = "spanId";
    public static final String KAFKA_PARENT_SPAN_ID_KEY = "parentSpanId";
    public static final String KAFKA_SAMPLED_KEY = "sampled";

    private LogConstants() {
        // Prevent instantiation
    }
}
