package com.bosch.iot.uok.agent.metrics;

import com.bosch.iot.uok.common.config.GrayConfig;
import com.bosch.iot.uok.common.config.SamplerConfig;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.degrade.DegradeManager;
import com.bosch.iot.uok.common.metrics.MetricCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ObservabilityController}.
 */
class ObservabilityControllerTest {

    private UokConfig config;
    private ObservabilityController controller;

    @BeforeEach
    void setUp() {
        config = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .agentEnabled(true)
                .traceEnabled(true)
                .metricsEnabled(true)
                .samplerConfig(new SamplerConfig())
                .grayConfig(new GrayConfig())
                .build();
        controller = new ObservabilityController(config);
    }

    // --- Construction / Getter Tests ---

    @Test
    @DisplayName("Should initialize all components from config")
    void shouldInitializeAllComponents() {
        assertThat(controller.getConfig()).isSameAs(config);
        assertThat(controller.getHeadSampler()).isNotNull();
        assertThat(controller.getErrorAlwaysSampler()).isNotNull();
        assertThat(controller.getTailSampler()).isNotNull();
        assertThat(controller.getDeviceRatioSampler()).isNotNull();
        assertThat(controller.getGrayController()).isNotNull();
        assertThat(controller.getDegradeManager()).isNotNull();
    }

    // --- shouldTrace Tests ---

    @Test
    @DisplayName("Should trace when all conditions pass")
    void shouldTraceWhenAllConditionsPass() {
        boolean result = controller.shouldTrace("test-service", "10.0.0.1", "some-trace-id");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not trace when agent is disabled")
    void shouldNotTraceWhenAgentDisabled() {
        UokConfig disabledConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .agentEnabled(false)
                .traceEnabled(true)
                .samplerConfig(new SamplerConfig())
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController disabledController = new ObservabilityController(disabledConfig);

        assertThat(disabledController.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();
    }

    @Test
    @DisplayName("Should not trace when tracing is disabled")
    void shouldNotTraceWhenTracingDisabled() {
        UokConfig noTraceConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .agentEnabled(true)
                .traceEnabled(false)
                .samplerConfig(new SamplerConfig())
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController noTraceController = new ObservabilityController(noTraceConfig);

        assertThat(noTraceController.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();
    }

    @Test
    @DisplayName("Should not trace when manual degradation is active")
    void shouldNotTraceWhenManualDegradationActive() {
        // Manual degradation disables everything
        controller.getDegradeManager().manualDegrade();

        assertThat(controller.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();
    }

    @Test
    @DisplayName("Should not trace when gray release excludes service")
    void shouldNotTraceWhenGrayExcludesService() {
        GrayConfig grayConfig = new GrayConfig();
        grayConfig.setServiceList(List.of("other-service"));
        UokConfig grayExcludedConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .agentEnabled(true)
                .traceEnabled(true)
                .samplerConfig(new SamplerConfig())
                .grayConfig(grayConfig)
                .build();
        ObservabilityController grayController = new ObservabilityController(grayExcludedConfig);

        assertThat(grayController.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();
    }

    @Test
    @DisplayName("Should not trace when head sampler rejects")
    void shouldNotTraceWhenSamplerRejects() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setHeadRate(0.0);
        UokConfig zeroRateConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .agentEnabled(true)
                .traceEnabled(true)
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController zeroController = new ObservabilityController(zeroRateConfig);

        assertThat(zeroController.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();
    }

    // --- shouldForceSampleError Tests ---

    @Test
    @DisplayName("Should force sample when error present and enabled")
    void shouldForceSampleWhenErrorPresent() {
        boolean result = controller.shouldForceSampleError("trace-id", true);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not force sample when no error")
    void shouldNotForceSampleWhenNoError() {
        boolean result = controller.shouldForceSampleError("trace-id", false);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should not force sample when errorAlways disabled")
    void shouldNotForceSampleWhenErrorAlwaysDisabled() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setErrorAlways(false);
        UokConfig noErrorConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController noErrorController = new ObservabilityController(noErrorConfig);

        assertThat(noErrorController.shouldForceSampleError("trace-id", true)).isFalse();
    }

    // --- shouldTailSample Tests ---

    @Test
    @DisplayName("Should tail sample slow traces when enabled")
    void shouldTailSampleSlowTracesWhenEnabled() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setTailSamplingEnabled(true);
        UokConfig tailConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController tailController = new ObservabilityController(tailConfig);

        boolean result = tailController.shouldTailSample(2000L, false);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should tail sample error traces when enabled")
    void shouldTailSampleErrorTracesWhenEnabled() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setTailSamplingEnabled(true);
        UokConfig tailConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController tailController = new ObservabilityController(tailConfig);

        boolean result = tailController.shouldTailSample(10L, true);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not tail sample when tail sampling disabled (default)")
    void shouldNotTailSampleWhenDisabled() {
        // Default SamplerConfig has tailSamplingEnabled=false
        boolean result = controller.shouldTailSample(2000L, false);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Should not tail sample fast successful traces when enabled")
    void shouldNotTailSampleFastSuccessfulTracesWhenEnabled() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setTailSamplingEnabled(true);
        UokConfig tailConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController tailController = new ObservabilityController(tailConfig);

        boolean result = tailController.shouldTailSample(50L, false);
        assertThat(result).isFalse();
    }

    // --- shouldSampleDevice Tests ---

    @Test
    @DisplayName("Should sample device with default 100% ratio")
    void shouldSampleDeviceWithDefaultRatio() {
        boolean result = controller.shouldSampleDevice("device-001");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should sample null device ID with 100% ratio")
    void shouldSampleNullDeviceWithFullRatio() {
        // With ratio=1.0, shouldSampleDevice returns true early
        boolean result = controller.shouldSampleDevice(null);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should not sample any device with 0% ratio")
    void shouldNotSampleAnyDeviceWithZeroRatio() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setDeviceRatio(0.0);
        UokConfig zeroConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .samplerConfig(samplerConfig)
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController zeroController = new ObservabilityController(zeroConfig);

        assertThat(zeroController.shouldSampleDevice("device-001")).isFalse();
    }

    // --- recordMetric Tests ---

    @Test
    @DisplayName("Should record metrics when metrics enabled")
    void shouldRecordMetricsWhenEnabled() {
        controller.recordMetric("test-service", 100L, true);
        controller.recordMetric("test-service", 200L, false);

        MetricCollector collector = controller.getMetricCollector("test-service");
        assertThat(collector).isNotNull();
    }

    @Test
    @DisplayName("Should not record metrics when metrics disabled")
    void shouldNotRecordMetricsWhenDisabled() {
        UokConfig noMetricsConfig = new UokConfig.Builder()
                .serviceName("test-service")
                .env("test")
                .metricsEnabled(false)
                .samplerConfig(new SamplerConfig())
                .grayConfig(new GrayConfig())
                .build();
        ObservabilityController noMetricsController = new ObservabilityController(noMetricsConfig);

        noMetricsController.recordMetric("test-service", 100L, true);
        assertThat(noMetricsController.getMetricCollector("test-service")).isNull();
    }

    @Test
    @DisplayName("Should create metric collector per service")
    void shouldCreateMetricCollectorPerService() {
        controller.recordMetric("service-a", 100L, true);
        controller.recordMetric("service-b", 200L, true);

        assertThat(controller.getMetricCollector("service-a")).isNotNull();
        assertThat(controller.getMetricCollector("service-b")).isNotNull();
        assertThat(controller.getMetricCollector("service-a"))
                .isNotSameAs(controller.getMetricCollector("service-b"));
    }

    // --- exportPrometheusMetrics Tests ---

    @Test
    @DisplayName("Should export Prometheus metrics for all services")
    void shouldExportPrometheusMetrics() {
        controller.recordMetric("service-a", 100L, true);
        controller.recordMetric("service-b", 200L, false);

        String prometheus = controller.exportPrometheusMetrics();
        assertThat(prometheus).isNotEmpty();
        assertThat(prometheus).contains("service-a");
        assertThat(prometheus).contains("service-b");
    }

    @Test
    @DisplayName("Should return empty string when no metrics recorded")
    void shouldReturnEmptyStringWhenNoMetrics() {
        String prometheus = controller.exportPrometheusMetrics();
        assertThat(prometheus).isEmpty();
    }

    // --- checkDegradation Tests ---

    @Test
    @DisplayName("Should auto-degrade on high CPU usage")
    void shouldAutoDegradeOnHighCpuUsage() {
        controller.checkDegradation(95.0, 10.0);

        DegradeManager dm = controller.getDegradeManager();
        assertThat(dm.isAutoDegraded()).isTrue();
    }

    @Test
    @DisplayName("Should not auto-degrade under normal conditions")
    void shouldNotAutoDegradeUnderNormalConditions() {
        controller.checkDegradation(30.0, 5.0);

        DegradeManager dm = controller.getDegradeManager();
        assertThat(dm.isAutoDegraded()).isFalse();
    }

    @Test
    @DisplayName("Should auto-recover when conditions improve")
    void shouldAutoRecoverWhenConditionsImprove() {
        controller.checkDegradation(95.0, 50.0);
        assertThat(controller.getDegradeManager().isAutoDegraded()).isTrue();

        controller.checkDegradation(20.0, 2.0);
        assertThat(controller.getDegradeManager().isAutoDegraded()).isFalse();
    }

    @Test
    @DisplayName("Should auto-degrade on high latency increase")
    void shouldAutoDegradeOnHighLatencyIncrease() {
        controller.checkDegradation(30.0, 200.0);

        DegradeManager dm = controller.getDegradeManager();
        assertThat(dm.isAutoDegraded()).isTrue();
    }

    @Test
    @DisplayName("Should not trace when manual degradation overrides auto-recovery")
    void shouldNotTraceWhenManualDegraded() {
        controller.getDegradeManager().manualDegrade();
        // Even with good conditions, manual degrade blocks tracing
        controller.checkDegradation(10.0, 1.0);
        assertThat(controller.shouldTrace("test-service", "10.0.0.1", "trace-id")).isFalse();

        // Recover manually
        controller.getDegradeManager().manualRecover();
        assertThat(controller.shouldTrace("test-service", "10.0.0.1", "trace-id")).isTrue();
    }

    // --- Edge Cases ---

    @Test
    @DisplayName("Should handle multiple degradation checks")
    void shouldHandleMultipleDegradationChecks() {
        // Both CPU=50 and latency=5 are below thresholds (80 and 10)
        controller.checkDegradation(50.0, 5.0);
        assertThat(controller.getDegradeManager().isAutoDegraded()).isFalse();

        controller.checkDegradation(95.0, 200.0);
        assertThat(controller.getDegradeManager().isAutoDegraded()).isTrue();

        controller.checkDegradation(10.0, 1.0);
        assertThat(controller.getDegradeManager().isAutoDegraded()).isFalse();
    }

    @Test
    @DisplayName("Should handle getMetricCollector for non-existent service")
    void shouldHandleNonExistentService() {
        assertThat(controller.getMetricCollector("non-existent")).isNull();
    }
}
