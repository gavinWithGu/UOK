package com.bosch.iot.uok.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UokConfig}.
 */
class UokConfigTest {

    @Test
    @DisplayName("Should create config with default values")
    void shouldCreateWithDefaults() {
        UokConfig config = new UokConfig();
        assertThat(config.getServiceName()).isEqualTo("unknown-service");
        assertThat(config.getBizDomain()).isEqualTo("default");
        assertThat(config.getTeamName()).isEqualTo("default");
        assertThat(config.isAgentEnabled()).isTrue();
        assertThat(config.isLogEnabled()).isTrue();
        assertThat(config.isTraceEnabled()).isTrue();
        assertThat(config.isMetricsEnabled()).isTrue();
        assertThat(config.getDegradeCpuThreshold()).isEqualTo(80);
        assertThat(config.getDegradeLatencyIncrease()).isEqualTo(10);
        assertThat(config.getSamplerConfig()).isNotNull();
        assertThat(config.getGrayConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should create config with builder")
    void shouldCreateWithBuilder() {
        UokConfig config = new UokConfig.Builder()
                .serviceName("test-service")
                .bizDomain("iot-home")
                .teamName("backend-team")
                .env("test")
                .agentEnabled(false)
                .logEnabled(false)
                .traceEnabled(false)
                .metricsEnabled(false)
                .degradeCpuThreshold(90)
                .degradeLatencyIncrease(20)
                .build();

        assertThat(config.getServiceName()).isEqualTo("test-service");
        assertThat(config.getBizDomain()).isEqualTo("iot-home");
        assertThat(config.getTeamName()).isEqualTo("backend-team");
        assertThat(config.getEnv()).isEqualTo("test");
        assertThat(config.isAgentEnabled()).isFalse();
        assertThat(config.isLogEnabled()).isFalse();
        assertThat(config.isTraceEnabled()).isFalse();
        assertThat(config.isMetricsEnabled()).isFalse();
        assertThat(config.getDegradeCpuThreshold()).isEqualTo(90);
        assertThat(config.getDegradeLatencyIncrease()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should create config with builder and custom sampler/gray config")
    void shouldCreateWithBuilderAndCustomConfigs() {
        SamplerConfig samplerConfig = new SamplerConfig();
        samplerConfig.setHeadRate(0.5);

        GrayConfig grayConfig = new GrayConfig();
        grayConfig.addService("svc-a");

        UokConfig config = new UokConfig.Builder()
                .serviceName("test-service")
                .samplerConfig(samplerConfig)
                .grayConfig(grayConfig)
                .build();

        assertThat(config.getSamplerConfig().getHeadRate()).isEqualTo(0.5);
        assertThat(config.getGrayConfig().getServiceList()).contains("svc-a");
    }

    @Test
    @DisplayName("Should set and get all properties")
    void shouldSetAndGetProperties() {
        UokConfig config = new UokConfig();
        config.setServiceName("my-service");
        config.setBizDomain("iot-device");
        config.setTeamName("platform-team");
        config.setEnv("prod");
        config.setAgentEnabled(false);
        config.setLogEnabled(false);
        config.setTraceEnabled(false);
        config.setMetricsEnabled(false);
        config.setDegradeCpuThreshold(70);
        config.setDegradeLatencyIncrease(5);

        assertThat(config.getServiceName()).isEqualTo("my-service");
        assertThat(config.getBizDomain()).isEqualTo("iot-device");
        assertThat(config.getTeamName()).isEqualTo("platform-team");
        assertThat(config.getEnv()).isEqualTo("prod");
        assertThat(config.isAgentEnabled()).isFalse();
        assertThat(config.isLogEnabled()).isFalse();
        assertThat(config.isTraceEnabled()).isFalse();
        assertThat(config.isMetricsEnabled()).isFalse();
        assertThat(config.getDegradeCpuThreshold()).isEqualTo(70);
        assertThat(config.getDegradeLatencyIncrease()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle null sampler config gracefully")
    void shouldHandleNullSamplerConfig() {
        UokConfig config = new UokConfig();
        config.setSamplerConfig(null);
        assertThat(config.getSamplerConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should handle null gray config gracefully")
    void shouldHandleNullGrayConfig() {
        UokConfig config = new UokConfig();
        config.setGrayConfig(null);
        assertThat(config.getGrayConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEquals() {
        UokConfig config1 = new UokConfig();
        config1.setServiceName("svc");
        config1.setEnv("test");

        UokConfig config2 = new UokConfig();
        config2.setServiceName("svc");
        config2.setEnv("test");

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToString() {
        UokConfig config = new UokConfig();
        String str = config.toString();
        assertThat(str).contains("unknown-service");
        assertThat(str).contains("default");
        assertThat(str).contains("agentEnabled=true");
    }

    @Test
    @DisplayName("Should detect environment from system property")
    void shouldDetectEnvFromSystemProperty() {
        String original = System.getProperty("uok.env");
        try {
            System.setProperty("uok.env", "staging");
            UokConfig config = new UokConfig();
            assertThat(config.getEnv()).isEqualTo("staging");
        } finally {
            if (original != null) {
                System.setProperty("uok.env", original);
            } else {
                System.clearProperty("uok.env");
            }
        }
    }

    @Test
    @DisplayName("Should detect environment from empty system property")
    void shouldDetectEnvFromEmptySystemProperty() {
        String original = System.getProperty("uok.env");
        try {
            System.setProperty("uok.env", "");
            UokConfig config = new UokConfig();
            // Empty string should fall through to next detection
            assertThat(config.getEnv()).isNotNull();
        } finally {
            if (original != null) {
                System.setProperty("uok.env", original);
            } else {
                System.clearProperty("uok.env");
            }
        }
    }

    @Test
    @DisplayName("Should detect environment from spring profiles - prod")
    void shouldDetectEnvFromSpringProd() {
        String original = System.getProperty("spring.profiles.active");
        try {
            System.clearProperty("uok.env");
            System.setProperty("spring.profiles.active", "prod");
            UokConfig config = new UokConfig();
            assertThat(config.getEnv()).isEqualTo("prod");
        } finally {
            if (original != null) {
                System.setProperty("spring.profiles.active", original);
            } else {
                System.clearProperty("spring.profiles.active");
            }
        }
    }

    @Test
    @DisplayName("Should detect environment from spring profiles - test")
    void shouldDetectEnvFromSpringTest() {
        String original = System.getProperty("spring.profiles.active");
        String uokOriginal = System.getProperty("uok.env");
        try {
            System.clearProperty("uok.env");
            System.setProperty("spring.profiles.active", "test");
            UokConfig config = new UokConfig();
            assertThat(config.getEnv()).isEqualTo("test");
        } finally {
            if (original != null) {
                System.setProperty("spring.profiles.active", original);
            } else {
                System.clearProperty("spring.profiles.active");
            }
            if (uokOriginal != null) {
                System.setProperty("uok.env", uokOriginal);
            } else {
                System.clearProperty("uok.env");
            }
        }
    }

    @Test
    @DisplayName("Equals should return false for different configs - all fields")
    void equalsShouldReturnFalseForDifferent() {
        UokConfig base = new UokConfig();

        // Different serviceName
        UokConfig diffSvc = new UokConfig();
        diffSvc.setServiceName("different");
        assertThat(base).isNotEqualTo(diffSvc);

        // Different bizDomain
        UokConfig diffBiz = new UokConfig();
        diffBiz.setBizDomain("different");
        assertThat(base).isNotEqualTo(diffBiz);

        // Different teamName
        UokConfig diffTeam = new UokConfig();
        diffTeam.setTeamName("different");
        assertThat(base).isNotEqualTo(diffTeam);

        // Different env
        UokConfig diffEnv = new UokConfig();
        diffEnv.setEnv("prod");
        assertThat(base).isNotEqualTo(diffEnv);

        // Different agentEnabled
        UokConfig diffAgent = new UokConfig();
        diffAgent.setAgentEnabled(false);
        assertThat(base).isNotEqualTo(diffAgent);

        // Different logEnabled
        UokConfig diffLog = new UokConfig();
        diffLog.setLogEnabled(false);
        assertThat(base).isNotEqualTo(diffLog);

        // Different traceEnabled
        UokConfig diffTrace = new UokConfig();
        diffTrace.setTraceEnabled(false);
        assertThat(base).isNotEqualTo(diffTrace);

        // Different metricsEnabled
        UokConfig diffMetrics = new UokConfig();
        diffMetrics.setMetricsEnabled(false);
        assertThat(base).isNotEqualTo(diffMetrics);

        // Different degradeCpuThreshold
        UokConfig diffCpu = new UokConfig();
        diffCpu.setDegradeCpuThreshold(90);
        assertThat(base).isNotEqualTo(diffCpu);

        // Different degradeLatencyIncrease
        UokConfig diffLatency = new UokConfig();
        diffLatency.setDegradeLatencyIncrease(20);
        assertThat(base).isNotEqualTo(diffLatency);

        // Different samplerConfig
        UokConfig diffSampler = new UokConfig();
        SamplerConfig sc = new SamplerConfig();
        sc.setHeadRate(0.5);
        diffSampler.setSamplerConfig(sc);
        assertThat(base).isNotEqualTo(diffSampler);

        // Different grayConfig
        UokConfig diffGray = new UokConfig();
        GrayConfig gc = new GrayConfig();
        gc.addService("svc-a");
        diffGray.setGrayConfig(gc);
        assertThat(base).isNotEqualTo(diffGray);
    }

    @Test
    @DisplayName("Equals should return false for null")
    void equalsShouldReturnFalseForNull() {
        UokConfig config = new UokConfig();
        assertThat(config).isNotEqualTo(null);
    }

    @Test
    @DisplayName("Equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
        UokConfig config = new UokConfig();
        assertThat(config).isNotEqualTo("string");
    }

    @Test
    @DisplayName("Equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
        UokConfig config = new UokConfig();
        assertThat(config).isEqualTo(config);
    }
}
