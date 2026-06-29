package com.bosch.iot.uok.common.constant;

/**
 * Metric name constants for Prometheus metrics collection.
 * All metric names follow Prometheus naming conventions.
 */
public final class MetricConstants {

    // --- Request Metrics ---
    public static final String REQUEST_TOTAL = "uok_request_total";
    public static final String REQUEST_DURATION = "uok_request_duration_seconds";
    public static final String REQUEST_ERRORS = "uok_request_errors_total";

    // --- HTTP Metrics ---
    public static final String HTTP_SERVER_REQUEST_TOTAL = "uok_http_server_request_total";
    public static final String HTTP_SERVER_REQUEST_DURATION = "uok_http_server_request_duration_seconds";
    public static final String HTTP_CLIENT_REQUEST_TOTAL = "uok_http_client_request_total";
    public static final String HTTP_CLIENT_REQUEST_DURATION = "uok_http_client_request_duration_seconds";

    // --- Kafka Metrics ---
    public static final String KAFKA_PRODUCER_TOTAL = "uok_kafka_producer_total";
    public static final String KAFKA_PRODUCER_DURATION = "uok_kafka_producer_duration_seconds";
    public static final String KAFKA_CONSUMER_TOTAL = "uok_kafka_consumer_total";
    public static final String KAFKA_CONSUMER_DURATION = "uok_kafka_consumer_duration_seconds";

    // --- Database Metrics ---
    public static final String DB_CALL_TOTAL = "uok_db_call_total";
    public static final String DB_CALL_DURATION = "uok_db_call_duration_seconds";

    // --- Cache Metrics ---
    public static final String CACHE_CALL_TOTAL = "uok_cache_call_total";
    public static final String CACHE_CALL_DURATION = "uok_cache_call_duration_seconds";

    // --- Agent Internal Metrics ---
    public static final String AGENT_CPU_USAGE = "uok_agent_cpu_usage_percent";
    public static final String AGENT_MEMORY_USAGE = "uok_agent_memory_usage_bytes";
    public static final String AGENT_LATENCY_INCREASE = "uok_agent_latency_increase_percent";
    public static final String AGENT_DEGRADE_STATUS = "uok_agent_degrade_status";

    // --- Label Names ---
    public static final String LABEL_SERVICE = "service";
    public static final String LABEL_ENV = "env";
    public static final String LABEL_METHOD = "method";
    public static final String LABEL_URI = "uri";
    public static final String LABEL_STATUS = "status";
    public static final String LABEL_OPERATION = "operation";
    public static final String LABEL_ERROR = "error";
    public static final String LABEL_DB_SYSTEM = "db_system";
    public static final String LABEL_CACHE_SYSTEM = "cache_system";
    public static final String LABEL_QUANTILE = "quantile";

    private MetricConstants() {
        // Prevent instantiation
    }
}
