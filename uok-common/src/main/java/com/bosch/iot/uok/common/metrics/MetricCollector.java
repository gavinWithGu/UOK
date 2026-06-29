package com.bosch.iot.uok.common.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Metric collector for service golden signals.
 * Collects request count, error count, duration metrics,
 * and computes QPS, error rate, and percentile latencies.
 * <p>
 * Designed to be exported in Prometheus format.
 */
public class MetricCollector {

    private final String serviceName;
    private final String env;

    private final AtomicLong requestTotal = new AtomicLong(0);
    private final AtomicLong errorTotal = new AtomicLong(0);
    private final DoubleAdder totalDurationMs = new DoubleAdder();
    private final ConcurrentHashMap<String, AtomicLong> labelCounters = new ConcurrentHashMap<>();

    // For percentile calculation, keep recent durations
    private final int maxSamples;
    private final long[] durationSamples;
    private volatile int sampleIndex = 0;
    private volatile int sampleCount = 0;

    /**
     * Create a metric collector.
     *
     * @param serviceName the service name
     * @param env         the environment
     * @param maxSamples  maximum number of duration samples to keep for percentile calculation
     */
    public MetricCollector(String serviceName, String env, int maxSamples) {
        this.serviceName = serviceName;
        this.env = env;
        this.maxSamples = Math.max(100, maxSamples);
        this.durationSamples = new long[this.maxSamples];
    }

    /**
     * Create a metric collector with default sample size.
     *
     * @param serviceName the service name
     * @param env         the environment
     */
    public MetricCollector(String serviceName, String env) {
        this(serviceName, env, 1000);
    }

    /**
     * Record a request with duration and success status.
     *
     * @param durationMs the request duration in milliseconds
     * @param success    whether the request was successful
     */
    public void recordRequest(long durationMs, boolean success) {
        requestTotal.incrementAndGet();
        totalDurationMs.add(durationMs);

        if (!success) {
            errorTotal.incrementAndGet();
        }

        // Store duration sample for percentile calculation
        int idx = sampleIndex;
        durationSamples[idx % maxSamples] = durationMs;
        sampleIndex = (idx + 1) % maxSamples;
        sampleCount = Math.min(sampleCount + 1, maxSamples);
    }

    /**
     * Record a request with labels.
     *
     * @param durationMs the request duration in milliseconds
     * @param success    whether the request was successful
     * @param labels     additional labels for the metric
     */
    public void recordRequest(long durationMs, boolean success, Map<String, String> labels) {
        recordRequest(durationMs, success);

        if (labels != null) {
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                String labelKey = entry.getKey() + "=" + entry.getValue();
                labelCounters.computeIfAbsent(labelKey, k -> new AtomicLong(0)).incrementAndGet();
            }
        }
    }

    /**
     * Get the total request count.
     *
     * @return total request count
     */
    public long getRequestTotal() {
        return requestTotal.get();
    }

    /**
     * Get the total error count.
     *
     * @return total error count
     */
    public long getErrorTotal() {
        return errorTotal.get();
    }

    /**
     * Get the error rate.
     *
     * @return error rate as a value between 0.0 and 1.0
     */
    public double getErrorRate() {
        long total = requestTotal.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) errorTotal.get() / total;
    }

    /**
     * Get the average request duration.
     *
     * @return average duration in milliseconds
     */
    public double getAverageDurationMs() {
        long total = requestTotal.get();
        if (total == 0) {
            return 0.0;
        }
        return totalDurationMs.sum() / total;
    }

    /**
     * Calculate the percentile duration.
     *
     * @param percentile the percentile to calculate (0.0 to 1.0)
     * @return the duration at the given percentile
     */
    public long getPercentileDuration(double percentile) {
        if (percentile < 0.0 || percentile > 1.0) {
            throw new IllegalArgumentException("Percentile must be between 0.0 and 1.0");
        }
        if (sampleCount == 0) {
            return 0;
        }

        long[] sorted = new long[sampleCount];
        int startIdx = Math.max(0, sampleIndex - sampleCount);
        for (int i = 0; i < sampleCount; i++) {
            sorted[i] = durationSamples[(startIdx + i) % maxSamples];
        }
        java.util.Arrays.sort(sorted);

        int index = (int) Math.ceil(percentile * sampleCount) - 1;
        return sorted[Math.max(0, Math.min(index, sampleCount - 1))];
    }

    /**
     * Get the P50 (median) duration.
     *
     * @return P50 duration in milliseconds
     */
    public long getP50() {
        return getPercentileDuration(0.50);
    }

    /**
     * Get the P95 duration.
     *
     * @return P95 duration in milliseconds
     */
    public long getP95() {
        return getPercentileDuration(0.95);
    }

    /**
     * Get the P99 duration.
     *
     * @return P99 duration in milliseconds
     */
    public long getP99() {
        return getPercentileDuration(0.99);
    }

    /**
     * Export metrics in Prometheus text format.
     *
     * @return Prometheus formatted metrics string
     */
    public String exportPrometheus() {
        StringBuilder sb = new StringBuilder();
        String serviceLabel = "service=\"" + serviceName + "\",env=\"" + env + "\"";

        sb.append("# HELP uok_request_total Total number of requests\n");
        sb.append("# TYPE uok_request_total counter\n");
        sb.append("uok_request_total{").append(serviceLabel).append("} ").append(requestTotal.get()).append("\n");

        sb.append("# HELP uok_request_errors_total Total number of request errors\n");
        sb.append("# TYPE uok_request_errors_total counter\n");
        sb.append("uok_request_errors_total{").append(serviceLabel).append("} ").append(errorTotal.get()).append("\n");

        sb.append("# HELP uok_request_duration_seconds Average request duration\n");
        sb.append("# TYPE uok_request_duration_seconds gauge\n");
        sb.append("uok_request_duration_seconds{").append(serviceLabel).append("} ")
                .append(String.format("%.6f", getAverageDurationMs() / 1000.0)).append("\n");

        return sb.toString();
    }

    /**
     * Reset all metrics counters.
     */
    public void reset() {
        requestTotal.set(0);
        errorTotal.set(0);
        totalDurationMs.reset();
        labelCounters.clear();
        sampleIndex = 0;
        sampleCount = 0;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getEnv() {
        return env;
    }
}
