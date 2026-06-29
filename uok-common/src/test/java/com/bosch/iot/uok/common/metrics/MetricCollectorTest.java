package com.bosch.iot.uok.common.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MetricCollector}.
 */
class MetricCollectorTest {

    private MetricCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MetricCollector("test-service", "test", 100);
    }

    @Test
    @DisplayName("Should record successful requests")
    void shouldRecordSuccessfulRequests() {
        collector.recordRequest(100, true);
        collector.recordRequest(200, true);
        collector.recordRequest(150, true);

        assertThat(collector.getRequestTotal()).isEqualTo(3);
        assertThat(collector.getErrorTotal()).isEqualTo(0);
        assertThat(collector.getErrorRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should record failed requests")
    void shouldRecordFailedRequests() {
        collector.recordRequest(100, true);
        collector.recordRequest(200, false);
        collector.recordRequest(150, false);

        assertThat(collector.getRequestTotal()).isEqualTo(3);
        assertThat(collector.getErrorTotal()).isEqualTo(2);
        assertThat(collector.getErrorRate()).isCloseTo(2.0 / 3.0, org.assertj.core.data.Offset.offset(0.001));
    }

    @Test
    @DisplayName("Should calculate average duration")
    void shouldCalculateAverageDuration() {
        collector.recordRequest(100, true);
        collector.recordRequest(200, true);
        collector.recordRequest(300, true);

        assertThat(collector.getAverageDurationMs()).isEqualTo(200.0);
    }

    @Test
    @DisplayName("Should return zero for empty collector")
    void shouldReturnZeroForEmptyCollector() {
        assertThat(collector.getRequestTotal()).isEqualTo(0);
        assertThat(collector.getErrorTotal()).isEqualTo(0);
        assertThat(collector.getErrorRate()).isEqualTo(0.0);
        assertThat(collector.getAverageDurationMs()).isEqualTo(0.0);
        assertThat(collector.getP50()).isEqualTo(0);
        assertThat(collector.getP95()).isEqualTo(0);
        assertThat(collector.getP99()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should calculate percentiles")
    void shouldCalculatePercentiles() {
        for (int i = 1; i <= 100; i++) {
            collector.recordRequest(i, true);
        }

        long p50 = collector.getP50();
        long p95 = collector.getP95();
        long p99 = collector.getP99();

        assertThat(p50).isGreaterThan(0);
        assertThat(p95).isGreaterThan(p50);
        assertThat(p99).isGreaterThanOrEqualTo(p95);
    }

    @Test
    @DisplayName("Should throw on invalid percentile")
    void shouldThrowOnInvalidPercentile() {
        assertThatThrownBy(() -> collector.getPercentileDuration(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> collector.getPercentileDuration(1.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should export Prometheus format")
    void shouldExportPrometheusFormat() {
        collector.recordRequest(100, true);
        collector.recordRequest(200, false);

        String prometheus = collector.exportPrometheus();
        assertThat(prometheus).contains("uok_request_total");
        assertThat(prometheus).contains("uok_request_errors_total");
        assertThat(prometheus).contains("uok_request_duration_seconds");
        assertThat(prometheus).contains("service=\"test-service\"");
        assertThat(prometheus).contains("env=\"test\"");
    }

    @Test
    @DisplayName("Should reset all counters")
    void shouldResetAllCounters() {
        collector.recordRequest(100, true);
        collector.recordRequest(200, false);

        collector.reset();

        assertThat(collector.getRequestTotal()).isEqualTo(0);
        assertThat(collector.getErrorTotal()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should record requests with labels")
    void shouldRecordRequestsWithLabels() {
        Map<String, String> labels = new HashMap<>();
        labels.put("method", "GET");
        labels.put("uri", "/api/v1/test");

        collector.recordRequest(100, true, labels);
        assertThat(collector.getRequestTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should handle null labels")
    void shouldHandleNullLabels() {
        collector.recordRequest(100, true, null);
        assertThat(collector.getRequestTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return service name and env")
    void shouldReturnServiceNameAndEnv() {
        assertThat(collector.getServiceName()).isEqualTo("test-service");
        assertThat(collector.getEnv()).isEqualTo("test");
    }

    @Test
    @DisplayName("Should handle min sample size")
    void shouldHandleMinSampleSize() {
        MetricCollector smallCollector = new MetricCollector("svc", "env", 50);
        // maxSamples should be at least 100
        smallCollector.recordRequest(100, true);
        assertThat(smallCollector.getRequestTotal()).isEqualTo(1);
    }
}
