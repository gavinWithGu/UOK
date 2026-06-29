package com.bosch.iot.uok.common.sampler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ErrorAlwaysSampler}.
 */
class ErrorAlwaysSamplerTest {

    @Test
    @DisplayName("Should always sample errors when enabled")
    void shouldAlwaysSampleErrorsWhenEnabled() {
        ErrorAlwaysSampler sampler = new ErrorAlwaysSampler(true);
        assertThat(sampler.shouldSampleError("trace-1", true)).isTrue();
    }

    @Test
    @DisplayName("Should not sample non-errors when hasError is false")
    void shouldNotSampleNonErrors() {
        ErrorAlwaysSampler sampler = new ErrorAlwaysSampler(true);
        assertThat(sampler.shouldSampleError("trace-1", false)).isFalse();
    }

    @Test
    @DisplayName("Should not sample when disabled")
    void shouldNotSampleWhenDisabled() {
        ErrorAlwaysSampler sampler = new ErrorAlwaysSampler(false);
        assertThat(sampler.shouldSampleError("trace-1", true)).isFalse();
    }

    @Test
    @DisplayName("Should sample when enabled with default constructor")
    void shouldSampleWithDefaultConstructor() {
        ErrorAlwaysSampler sampler = new ErrorAlwaysSampler();
        assertThat(sampler.shouldSample("trace-1")).isTrue();
        assertThat(sampler.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should return correct description")
    void shouldReturnCorrectDescription() {
        ErrorAlwaysSampler sampler = new ErrorAlwaysSampler(true);
        assertThat(sampler.getDescription()).contains("enabled=true");
    }
}
