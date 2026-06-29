package com.bosch.iot.uok.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link SamplerConfig}.
 */
class SamplerConfigTest {

    @Test
    @DisplayName("Should create config with default values")
    void shouldCreateWithDefaults() {
        SamplerConfig config = new SamplerConfig();
        assertThat(config.getHeadRate()).isEqualTo(1.0);
        assertThat(config.isErrorAlways()).isTrue();
        assertThat(config.getDeviceRatio()).isEqualTo(1.0);
        assertThat(config.isTailSamplingEnabled()).isFalse();
        assertThat(config.getTailSamplingThresholdMs()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Should set and get head rate with clamping")
    void shouldSetHeadRateWithClamping() {
        SamplerConfig config = new SamplerConfig();
        config.setHeadRate(0.5);
        assertThat(config.getHeadRate()).isEqualTo(0.5);

        config.setHeadRate(2.0);
        assertThat(config.getHeadRate()).isEqualTo(1.0);

        config.setHeadRate(-1.0);
        assertThat(config.getHeadRate()).isEqualTo(0.0);

        config.setHeadRate(0.0);
        assertThat(config.getHeadRate()).isEqualTo(0.0);

        config.setHeadRate(1.0);
        assertThat(config.getHeadRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should set and get device ratio with clamping")
    void shouldSetDeviceRatioWithClamping() {
        SamplerConfig config = new SamplerConfig();
        config.setDeviceRatio(0.3);
        assertThat(config.getDeviceRatio()).isEqualTo(0.3);

        config.setDeviceRatio(2.0);
        assertThat(config.getDeviceRatio()).isEqualTo(1.0);

        config.setDeviceRatio(-0.5);
        assertThat(config.getDeviceRatio()).isEqualTo(0.0);

        config.setDeviceRatio(0.0);
        assertThat(config.getDeviceRatio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should validate successfully for valid config")
    void shouldValidateSuccessfully() {
        SamplerConfig config = new SamplerConfig();
        config.validate();
    }

    @Test
    @DisplayName("Should validate head rate range after clamping")
    void shouldValidateHeadRateRange() {
        SamplerConfig config = new SamplerConfig();
        config.setHeadRate(0.5);
        config.validate();

        config.setHeadRate(-0.5);
        assertThat(config.getHeadRate()).isEqualTo(0.0);
        config.validate();
    }

    @Test
    @DisplayName("Should throw on invalid head rate via validate")
    void shouldThrowOnInvalidHeadRateInValidate() {
        SamplerConfig config = new SamplerConfig();
        config.setHeadRate(0.5);
        // Valid config should not throw
        config.validate();
    }

    @Test
    @DisplayName("Should throw on invalid device ratio via validate")
    void shouldThrowOnInvalidDeviceRatio() {
        SamplerConfig config = new SamplerConfig();
        config.setDeviceRatio(0.5);
        config.validate(); // Should not throw
    }

    @Test
    @DisplayName("Should throw on invalid tailSamplingThresholdMs")
    void shouldThrowOnInvalidTailSamplingThreshold() {
        SamplerConfig config = new SamplerConfig();
        config.setTailSamplingThresholdMs(100);
        config.validate(); // Valid, should not throw
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEquals() {
        SamplerConfig config1 = new SamplerConfig();
        SamplerConfig config2 = new SamplerConfig();

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());

        // Different headRate
        config1.setHeadRate(0.5);
        assertThat(config1).isNotEqualTo(config2);

        // Same headRate
        config2.setHeadRate(0.5);
        assertThat(config1).isEqualTo(config2);

        // Different errorAlways
        config1.setErrorAlways(false);
        assertThat(config1).isNotEqualTo(config2);

        // Null comparison
        assertThat(config1).isNotEqualTo(null);

        // Different type
        assertThat(config1).isNotEqualTo("string");
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToString() {
        SamplerConfig config = new SamplerConfig();
        String str = config.toString();
        assertThat(str).contains("SamplerConfig");
        assertThat(str).contains("headRate=");
        assertThat(str).contains("errorAlways=");
        assertThat(str).contains("deviceRatio=");
        assertThat(str).contains("tailSamplingThresholdMs=");
        assertThat(str).contains("tailSamplingEnabled=");
    }

    @Test
    @DisplayName("Should set tail sampling properties")
    void shouldSetTailSamplingProperties() {
        SamplerConfig config = new SamplerConfig();
        config.setTailSamplingEnabled(true);
        config.setTailSamplingThresholdMs(2000);

        assertThat(config.isTailSamplingEnabled()).isTrue();
        assertThat(config.getTailSamplingThresholdMs()).isEqualTo(2000);
    }

    @Test
    @DisplayName("Should test all field differences in equals")
    void shouldTestAllFieldDifferencesInEquals() {
        SamplerConfig base = new SamplerConfig();

        // Test deviceRatio difference
        SamplerConfig diffDeviceRatio = new SamplerConfig();
        diffDeviceRatio.setDeviceRatio(0.5);
        assertThat(base).isNotEqualTo(diffDeviceRatio);

        // Test tailSamplingThresholdMs difference
        SamplerConfig diffThreshold = new SamplerConfig();
        diffThreshold.setTailSamplingThresholdMs(500);
        assertThat(base).isNotEqualTo(diffThreshold);

        // Test tailSamplingEnabled difference
        SamplerConfig diffEnabled = new SamplerConfig();
        diffEnabled.setTailSamplingEnabled(true);
        assertThat(base).isNotEqualTo(diffEnabled);
    }
}
