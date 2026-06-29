package com.bosch.iot.uok.common.sampler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TailSampler}.
 */
class TailSamplerTest {

    @Test
    @DisplayName("Should sample slow traces when enabled")
    void shouldSampleSlowTraces() {
        TailSampler sampler = new TailSampler(true, 1000, true);
        assertThat(sampler.shouldSampleTail(1500, false)).isTrue();
        assertThat(sampler.shouldSampleTail(500, false)).isFalse();
    }

    @Test
    @DisplayName("Should sample error traces when enabled")
    void shouldSampleErrorTraces() {
        TailSampler sampler = new TailSampler(true, 1000, true);
        assertThat(sampler.shouldSampleTail(100, true)).isTrue();
    }

    @Test
    @DisplayName("Should not sample errors when sampleErrors is false")
    void shouldNotSampleErrorsWhenDisabled() {
        TailSampler sampler = new TailSampler(true, 1000, false);
        assertThat(sampler.shouldSampleTail(100, true)).isFalse();
    }

    @Test
    @DisplayName("Should not sample when disabled")
    void shouldNotSampleWhenDisabled() {
        TailSampler sampler = new TailSampler(false, 1000, true);
        assertThat(sampler.shouldSampleTail(5000, true)).isFalse();
    }

    @Test
    @DisplayName("Should create with default constructor")
    void shouldCreateWithDefaultConstructor() {
        TailSampler sampler = new TailSampler();
        assertThat(sampler.isEnabled()).isFalse();
        assertThat(sampler.getDurationThresholdMs()).isEqualTo(1000);
        assertThat(sampler.isSampleErrors()).isTrue();
    }

    @Test
    @DisplayName("Should always return false for base shouldSample")
    void shouldReturnFalseForBaseShouldSample() {
        TailSampler sampler = new TailSampler(true, 1000, true);
        assertThat(sampler.shouldSample("trace-1")).isFalse();
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        TailSampler sampler = new TailSampler(true, 500, false);
        String desc = sampler.getDescription();
        assertThat(desc).contains("enabled=true");
        assertThat(desc).contains("500");
    }

    @Test
    @DisplayName("Should sample trace at exact threshold")
    void shouldSampleAtExactThreshold() {
        TailSampler sampler = new TailSampler(true, 1000, false);
        assertThat(sampler.shouldSampleTail(1000, false)).isTrue();
        assertThat(sampler.shouldSampleTail(999, false)).isFalse();
    }
}
