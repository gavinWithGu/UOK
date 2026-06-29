package com.bosch.iot.uok.agent.config;

import com.bosch.iot.uok.common.config.UokConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AgentConfig}.
 */
class AgentConfigTest {

    @Test
    @DisplayName("Should load config from AgentConfig")
    void shouldLoadConfig() {
        UokConfig config = AgentConfig.load();
        assertThat(config).isNotNull();
        assertThat(config.getServiceName()).isNotNull();
    }

    @Test
    @DisplayName("Should set OTel resource attributes from config")
    void shouldSetOtelResourceAttributes() {
        UokConfig config = AgentConfig.load();
        String resourceAttrs = System.getProperty("otel.resource.attributes");
        assertThat(resourceAttrs).contains("service.name=" + config.getServiceName());
        assertThat(resourceAttrs).contains("deployment.environment=" + config.getEnv());
    }

    @Test
    @DisplayName("Should apply OTLP endpoint if configured")
    void shouldApplyOtlpEndpoint() {
        String original = System.getProperty("uok.otlp.endpoint");
        try {
            System.setProperty("uok.otlp.endpoint", "http://localhost:4317");
            AgentConfig.load();
            assertThat(System.getProperty("otel.exporter.otlp.endpoint"))
                    .isEqualTo("http://localhost:4317");
        } finally {
            if (original != null) {
                System.setProperty("uok.otlp.endpoint", original);
            } else {
                System.clearProperty("uok.otlp.endpoint");
            }
            System.clearProperty("otel.exporter.otlp.endpoint");
        }
    }

    @Test
    @DisplayName("Should apply Prometheus port if configured")
    void shouldApplyPrometheusPort() {
        String original = System.getProperty("uok.prometheus.port");
        try {
            System.setProperty("uok.prometheus.port", "9464");
            AgentConfig.load();
            assertThat(System.getProperty("otel.exporter.prometheus.port")).isEqualTo("9464");
        } finally {
            if (original != null) {
                System.setProperty("uok.prometheus.port", original);
            } else {
                System.clearProperty("uok.prometheus.port");
            }
            System.clearProperty("otel.exporter.prometheus.port");
        }
    }

    @Test
    @DisplayName("Should set sampler when head rate < 1.0")
    void shouldSetSamplerWhenHeadRateBelowOne() {
        String originalRate = System.getProperty("uok.sampler.head-rate");
        try {
            System.setProperty("uok.sampler.head-rate", "0.5");
            AgentConfig.load();
            assertThat(System.getProperty("otel.traces.sampler")).isEqualTo("traceidratio");
            assertThat(System.getProperty("otel.traces.sampler.arg")).isEqualTo("0.5");
        } finally {
            if (originalRate != null) {
                System.setProperty("uok.sampler.head-rate", originalRate);
            } else {
                System.clearProperty("uok.sampler.head-rate");
            }
            System.clearProperty("otel.traces.sampler");
            System.clearProperty("otel.traces.sampler.arg");
        }
    }
}
