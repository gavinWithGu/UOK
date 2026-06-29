package com.bosch.iot.uok.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ConfigLoader}.
 */
class ConfigLoaderTest {

    @BeforeEach
    void clearUokProperties() {
        // Clear UOK system properties before each test
        System.clearProperty("uok.service.name");
        System.clearProperty("uok.biz.domain");
        System.clearProperty("uok.team.name");
        System.clearProperty("uok.env");
        System.clearProperty("uok.agent.enabled");
        System.clearProperty("uok.log.enable");
        System.clearProperty("uok.trace.enable");
        System.clearProperty("uok.metrics.enable");
        System.clearProperty("uok.sampler.head-rate");
        System.clearProperty("uok.sampler.error-always");
        System.clearProperty("uok.sampler.device-ratio");
        System.clearProperty("uok.degrade.cpu-threshold");
        System.clearProperty("uok.degrade.latency-increase");
        System.clearProperty("uok.gray.service-list");
        System.clearProperty("uok.gray.instance-ratio");
    }

    @AfterEach
    void cleanup() {
        clearUokProperties();
    }

    @Test
    @DisplayName("Should load config with default values")
    void shouldLoadWithDefaults() {
        UokConfig config = ConfigLoader.load();
        assertThat(config).isNotNull();
        assertThat(config.getServiceName()).isEqualTo("unknown-service");
        assertThat(config.getBizDomain()).isEqualTo("default");
        assertThat(config.isAgentEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should load service name from system property")
    void shouldLoadServiceNameFromSystemProperty() {
        System.setProperty("uok.service.name", "my-service");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getServiceName()).isEqualTo("my-service");
    }

    @Test
    @DisplayName("Should load boolean config from system property")
    void shouldLoadBooleanFromSystemProperty() {
        System.setProperty("uok.agent.enabled", "false");
        UokConfig config = ConfigLoader.load();
        assertThat(config.isAgentEnabled()).isFalse();
    }

    @Test
    @DisplayName("Should load integer config from system property")
    void shouldLoadIntFromSystemProperty() {
        System.setProperty("uok.degrade.cpu-threshold", "90");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getDegradeCpuThreshold()).isEqualTo(90);
    }

    @Test
    @DisplayName("Should load sampler config from system property")
    void shouldLoadSamplerConfigFromSystemProperty() {
        System.setProperty("uok.sampler.head-rate", "0.5");
        System.setProperty("uok.sampler.error-always", "false");
        System.setProperty("uok.sampler.device-ratio", "0.3");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getSamplerConfig().getHeadRate()).isEqualTo(0.5);
        assertThat(config.getSamplerConfig().isErrorAlways()).isFalse();
        assertThat(config.getSamplerConfig().getDeviceRatio()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("Should load gray config from system property")
    void shouldLoadGrayConfigFromSystemProperty() {
        System.setProperty("uok.gray.service-list", "svc-a,svc-b");
        System.setProperty("uok.gray.instance-ratio", "0.5");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getGrayConfig().getServiceList()).containsExactly("svc-a", "svc-b");
        assertThat(config.getGrayConfig().getInstanceRatio()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Should handle invalid integer gracefully")
    void shouldHandleInvalidInteger() {
        System.setProperty("uok.degrade.cpu-threshold", "not-a-number");
        UokConfig config = ConfigLoader.load();
        // Should fall back to default
        assertThat(config.getDegradeCpuThreshold()).isEqualTo(80);
    }

    @Test
    @DisplayName("Should handle invalid double gracefully")
    void shouldHandleInvalidDouble() {
        System.setProperty("uok.sampler.head-rate", "not-a-number");
        UokConfig config = ConfigLoader.load();
        // Should fall back to default
        assertThat(config.getSamplerConfig().getHeadRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should load config from file when file does not exist")
    void shouldFallBackWhenFileNotExists() {
        UokConfig config = ConfigLoader.loadFromFile("/nonexistent/path/uok.properties");
        assertThat(config).isNotNull();
        assertThat(config.getServiceName()).isEqualTo("unknown-service");
    }

    @Test
    @DisplayName("Should handle empty gray service list")
    void shouldHandleEmptyGrayServiceList() {
        System.setProperty("uok.gray.service-list", "");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getGrayConfig().getServiceList()).isEmpty();
    }

    @Test
    @DisplayName("Should handle gray service list with whitespace")
    void shouldHandleGrayServiceListWithWhitespace() {
        System.setProperty("uok.gray.service-list", " svc-a , svc-b , ");
        UokConfig config = ConfigLoader.load();
        assertThat(config.getGrayConfig().getServiceList()).containsExactly("svc-a", "svc-b");
    }
}
