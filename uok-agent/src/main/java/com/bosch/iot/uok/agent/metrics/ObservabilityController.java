package com.bosch.iot.uok.agent.metrics;

import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.MetricConstants;
import com.bosch.iot.uok.common.degrade.DegradeManager;
import com.bosch.iot.uok.common.gray.GrayController;
import com.bosch.iot.uok.common.metrics.MetricCollector;
import com.bosch.iot.uok.common.sampler.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observability controller that integrates sampling, gray release,
 * degradation, and metrics collection into the agent.
 * <p>
 * This is the central coordinator for all observability control decisions:
 * - Should this request be traced? (sampling + gray)
 * - Is tracing currently degraded? (degradation)
 * - What metrics should be collected? (metrics)
 */
public class ObservabilityController {

    private final UokConfig config;
    private final HeadSampler headSampler;
    private final ErrorAlwaysSampler errorAlwaysSampler;
    private final TailSampler tailSampler;
    private final DeviceRatioSampler deviceRatioSampler;
    private final GrayController grayController;
    private final DegradeManager degradeManager;
    private final ConcurrentHashMap<String, MetricCollector> metricCollectors;

    /**
     * Create an ObservabilityController with the given configuration.
     *
     * @param config the UOK configuration
     */
    public ObservabilityController(UokConfig config) {
        this.config = config;
        this.headSampler = new HeadSampler(config.getSamplerConfig().getHeadRate());
        this.errorAlwaysSampler = new ErrorAlwaysSampler(config.getSamplerConfig().isErrorAlways());
        this.tailSampler = new TailSampler(
                config.getSamplerConfig().isTailSamplingEnabled(),
                config.getSamplerConfig().getTailSamplingThresholdMs(),
                true);
        this.deviceRatioSampler = new DeviceRatioSampler(config.getSamplerConfig().getDeviceRatio());
        this.grayController = new GrayController(config.getGrayConfig());
        this.degradeManager = new DegradeManager(
                config.getDegradeCpuThreshold(),
                config.getDegradeLatencyIncrease());
        this.metricCollectors = new ConcurrentHashMap<>();
    }

    /**
     * Determine if a request should be traced based on all control factors:
     * 1. Agent enabled check
     * 2. Trace enabled check
     * 3. Degradation check
     * 4. Gray release check
     * 5. Sampling check
     *
     * @param serviceName the service name
     * @param instanceIp  the instance IP
     * @param traceId     the trace ID (for sampling decision)
     * @return true if the request should be traced
     */
    public boolean shouldTrace(String serviceName, String instanceIp, String traceId) {
        // 1. Check if agent is enabled
        if (!config.isAgentEnabled() || !config.isTraceEnabled()) {
            return false;
        }

        // 2. Check degradation
        if (!degradeManager.isTracingActive()) {
            return false;
        }

        // 3. Check gray release
        if (!grayController.isInstrumentationEnabled(serviceName, instanceIp, null)) {
            return false;
        }

        // 4. Check head sampling
        return headSampler.shouldSample(traceId);
    }

    /**
     * Determine if an error should force sampling.
     *
     * @param traceId  the trace ID
     * @param hasError whether the trace has an error
     * @return true if the error should force sampling
     */
    public boolean shouldForceSampleError(String traceId, boolean hasError) {
        return errorAlwaysSampler.shouldSampleError(traceId, hasError);
    }

    /**
     * Determine if tail sampling should apply.
     *
     * @param durationMs the trace duration
     * @param hasError   whether the trace has an error
     * @return true if tail sampling should apply
     */
    public boolean shouldTailSample(long durationMs, boolean hasError) {
        return tailSampler.shouldSampleTail(durationMs, hasError);
    }

    /**
     * Determine if a device should be sampled.
     *
     * @param deviceId the device ID
     * @return true if the device should be sampled
     */
    public boolean shouldSampleDevice(String deviceId) {
        return deviceRatioSampler.shouldSampleDevice(deviceId);
    }

    /**
     * Record a request metric.
     *
     * @param serviceName the service name
     * @param durationMs  the request duration
     * @param success     whether the request succeeded
     */
    public void recordMetric(String serviceName, long durationMs, boolean success) {
        if (!config.isMetricsEnabled()) {
            return;
        }
        MetricCollector collector = metricCollectors.computeIfAbsent(
                serviceName, k -> new MetricCollector(k, config.getEnv()));
        collector.recordRequest(durationMs, success);
    }

    /**
     * Get the metric collector for a service.
     *
     * @param serviceName the service name
     * @return the metric collector
     */
    public MetricCollector getMetricCollector(String serviceName) {
        return metricCollectors.get(serviceName);
    }

    /**
     * Get Prometheus-format metrics output for all services.
     *
     * @return Prometheus text output
     */
    public String exportPrometheusMetrics() {
        StringBuilder sb = new StringBuilder();
        for (MetricCollector collector : metricCollectors.values()) {
            sb.append(collector.exportPrometheus());
        }
        return sb.toString();
    }

    /**
     * Check degradation and update status.
     *
     * @param cpuUsage       current CPU usage percentage
     * @param latencyIncrease current latency increase percentage
     */
    public void checkDegradation(double cpuUsage, double latencyIncrease) {
        degradeManager.checkAndDegrade(cpuUsage, latencyIncrease);
    }

    // --- Getters ---

    public UokConfig getConfig() { return config; }
    public HeadSampler getHeadSampler() { return headSampler; }
    public ErrorAlwaysSampler getErrorAlwaysSampler() { return errorAlwaysSampler; }
    public TailSampler getTailSampler() { return tailSampler; }
    public DeviceRatioSampler getDeviceRatioSampler() { return deviceRatioSampler; }
    public GrayController getGrayController() { return grayController; }
    public DegradeManager getDegradeManager() { return degradeManager; }
}
