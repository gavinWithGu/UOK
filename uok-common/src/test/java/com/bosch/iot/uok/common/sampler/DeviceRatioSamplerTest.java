package com.bosch.iot.uok.common.sampler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link DeviceRatioSampler}.
 */
class DeviceRatioSamplerTest {

    @Test
    @DisplayName("Should sample all devices at 100% ratio")
    void shouldSampleAllAtFullRatio() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(1.0);
        assertThat(sampler.shouldSampleDevice("device-1")).isTrue();
        assertThat(sampler.shouldSampleDevice("device-2")).isTrue();
    }

    @Test
    @DisplayName("Should not sample any devices at 0% ratio")
    void shouldNotSampleAnyAtZeroRatio() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.0);
        assertThat(sampler.shouldSampleDevice("device-1")).isFalse();
    }

    @Test
    @DisplayName("Should consistently sample the same device")
    void shouldConsistentlySampleSameDevice() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.5);
        String deviceId = "consistent-device-id";

        boolean firstResult = sampler.shouldSampleDevice(deviceId);
        // Same device should always produce the same result
        for (int i = 0; i < 100; i++) {
            assertThat(sampler.shouldSampleDevice(deviceId)).isEqualTo(firstResult);
        }
    }

    @Test
    @DisplayName("Should sample approximately at the configured ratio")
    void shouldSampleApproximatelyAtRatio() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.3);
        int sampledCount = 0;
        int totalDevices = 10000;
        for (int i = 0; i < totalDevices; i++) {
            if (sampler.shouldSampleDevice("device-" + i)) {
                sampledCount++;
            }
        }
        // With 10000 devices at 30%, expect 2500-3500
        assertThat(sampledCount).isBetween(2000, 4000);
    }

    @Test
    @DisplayName("Should handle null device ID")
    void shouldHandleNullDeviceId() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.5);
        assertThat(sampler.shouldSampleDevice(null)).isTrue();
    }

    @Test
    @DisplayName("Should handle empty device ID")
    void shouldHandleEmptyDeviceId() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.5);
        assertThat(sampler.shouldSampleDevice("")).isTrue();
    }

    @Test
    @DisplayName("Should throw on invalid ratio")
    void shouldThrowOnInvalidRatio() {
        assertThatThrownBy(() -> new DeviceRatioSampler(1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeviceRatioSampler(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should create default sampler with 100% ratio")
    void shouldCreateDefault() {
        DeviceRatioSampler sampler = new DeviceRatioSampler();
        assertThat(sampler.getRatio()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.5);
        assertThat(sampler.getDescription()).contains("0.5");
    }

    @Test
    @DisplayName("Base shouldSample returns true at 100%")
    void shouldSampleBaseAtFullRatio() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(1.0);
        assertThat(sampler.shouldSample("trace-1")).isTrue();
    }

    @Test
    @DisplayName("Base shouldSample returns false at less than 100%")
    void shouldSampleBaseNotFull() {
        DeviceRatioSampler sampler = new DeviceRatioSampler(0.5);
        assertThat(sampler.shouldSample("trace-1")).isFalse();
    }
}
