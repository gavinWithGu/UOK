package com.bosch.iot.uok.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MetricConstants}.
 * Verifies metric name constants are correctly defined.
 */
class MetricConstantsTest {

    @Test
    @DisplayName("Should define request metrics")
    void shouldDefineRequestMetrics() {
        assertThat(MetricConstants.REQUEST_TOTAL).isEqualTo("uok_request_total");
        assertThat(MetricConstants.REQUEST_DURATION).isEqualTo("uok_request_duration_seconds");
        assertThat(MetricConstants.REQUEST_ERRORS).isEqualTo("uok_request_errors_total");
    }

    @Test
    @DisplayName("Should define HTTP metrics")
    void shouldDefineHttpMetrics() {
        assertThat(MetricConstants.HTTP_SERVER_REQUEST_TOTAL).isEqualTo("uok_http_server_request_total");
        assertThat(MetricConstants.HTTP_SERVER_REQUEST_DURATION).isEqualTo("uok_http_server_request_duration_seconds");
        assertThat(MetricConstants.HTTP_CLIENT_REQUEST_TOTAL).isEqualTo("uok_http_client_request_total");
        assertThat(MetricConstants.HTTP_CLIENT_REQUEST_DURATION).isEqualTo("uok_http_client_request_duration_seconds");
    }

    @Test
    @DisplayName("Should define Kafka metrics")
    void shouldDefineKafkaMetrics() {
        assertThat(MetricConstants.KAFKA_PRODUCER_TOTAL).isEqualTo("uok_kafka_producer_total");
        assertThat(MetricConstants.KAFKA_CONSUMER_TOTAL).isEqualTo("uok_kafka_consumer_total");
    }

    @Test
    @DisplayName("Should define database metrics")
    void shouldDefineDatabaseMetrics() {
        assertThat(MetricConstants.DB_CALL_TOTAL).isEqualTo("uok_db_call_total");
        assertThat(MetricConstants.DB_CALL_DURATION).isEqualTo("uok_db_call_duration_seconds");
    }

    @Test
    @DisplayName("Should define cache metrics")
    void shouldDefineCacheMetrics() {
        assertThat(MetricConstants.CACHE_CALL_TOTAL).isEqualTo("uok_cache_call_total");
        assertThat(MetricConstants.CACHE_CALL_DURATION).isEqualTo("uok_cache_call_duration_seconds");
    }

    @Test
    @DisplayName("Should define agent internal metrics")
    void shouldDefineAgentInternalMetrics() {
        assertThat(MetricConstants.AGENT_CPU_USAGE).isEqualTo("uok_agent_cpu_usage_percent");
        assertThat(MetricConstants.AGENT_MEMORY_USAGE).isEqualTo("uok_agent_memory_usage_bytes");
        assertThat(MetricConstants.AGENT_LATENCY_INCREASE).isEqualTo("uok_agent_latency_increase_percent");
        assertThat(MetricConstants.AGENT_DEGRADE_STATUS).isEqualTo("uok_agent_degrade_status");
    }

    @Test
    @DisplayName("Should define label names")
    void shouldDefineLabelNames() {
        assertThat(MetricConstants.LABEL_SERVICE).isEqualTo("service");
        assertThat(MetricConstants.LABEL_ENV).isEqualTo("env");
        assertThat(MetricConstants.LABEL_METHOD).isEqualTo("method");
        assertThat(MetricConstants.LABEL_URI).isEqualTo("uri");
        assertThat(MetricConstants.LABEL_STATUS).isEqualTo("status");
    }

    @Test
    @DisplayName("Metric names should follow Prometheus naming convention")
    void shouldFollowPrometheusNamingConvention() {
        // Prometheus metric names should contain only [a-zA-Z0-9_:]
        String[] metricNames = {
                MetricConstants.REQUEST_TOTAL,
                MetricConstants.REQUEST_DURATION,
                MetricConstants.HTTP_SERVER_REQUEST_TOTAL,
                MetricConstants.KAFKA_PRODUCER_TOTAL,
                MetricConstants.DB_CALL_TOTAL,
                MetricConstants.AGENT_CPU_USAGE
        };

        for (String name : metricNames) {
            assertThat(name).matches("[a-zA-Z_:][a-zA-Z0-9_:]*");
        }
    }
}
