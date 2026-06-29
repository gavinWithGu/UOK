package com.bosch.iot.uok.common.sampler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HeadSampler}.
 */
class HeadSamplerTest {

    @Test
    @DisplayName("Should always sample when rate is 1.0")
    void shouldAlwaysSampleAtFullRate() {
        HeadSampler sampler = new HeadSampler(1.0);
        for (int i = 0; i < 100; i++) {
            assertThat(sampler.shouldSample("trace-" + i)).isTrue();
        }
    }

    @Test
    @DisplayName("Should never sample when rate is 0.0")
    void shouldNeverSampleAtZeroRate() {
        HeadSampler sampler = new HeadSampler(0.0);
        for (int i = 0; i < 100; i++) {
            assertThat(sampler.shouldSample("trace-" + i)).isFalse();
        }
    }

    @Test
    @DisplayName("Should sample approximately at the configured rate")
    void shouldSampleApproximatelyAtRate() {
        HeadSampler sampler = new HeadSampler(0.5);
        int sampledCount = 0;
        int totalSamples = 10000;
        for (int i = 0; i < totalSamples; i++) {
            if (sampler.shouldSample("trace-" + i)) {
                sampledCount++;
            }
        }
        // With 10000 samples at 50% rate, expect 4500-5500
        assertThat(sampledCount).isBetween(4000, 6000);
    }

    @Test
    @DisplayName("Should create default sampler with 100% rate")
    void shouldCreateDefaultSampler() {
        HeadSampler sampler = new HeadSampler();
        assertThat(sampler.getRate()).isEqualTo(1.0);
        assertThat(sampler.shouldSample("trace-1")).isTrue();
    }

    @Test
    @DisplayName("Should throw on invalid rate")
    void shouldThrowOnInvalidRate() {
        assertThatThrownBy(() -> new HeadSampler(1.5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new HeadSampler(-0.1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        HeadSampler sampler = new HeadSampler(0.5);
        assertThat(sampler.getDescription()).contains("0.5");
    }
}
