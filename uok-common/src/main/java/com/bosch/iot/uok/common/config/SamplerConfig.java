package com.bosch.iot.uok.common.config;

import java.util.Objects;

/**
 * Sampling configuration.
 * Controls trace sampling rates and strategies.
 */
public class SamplerConfig {

    private double headRate = 1.0;
    private boolean errorAlways = true;
    private double deviceRatio = 1.0;
    private long tailSamplingThresholdMs = 1000;
    private boolean tailSamplingEnabled = false;

    public SamplerConfig() {
    }

    /**
     * Validate the sampler configuration.
     *
     * @throws IllegalArgumentException if any configuration value is invalid
     */
    public void validate() {
        if (headRate < 0.0 || headRate > 1.0) {
            throw new IllegalArgumentException("headRate must be between 0.0 and 1.0, got: " + headRate);
        }
        if (deviceRatio < 0.0 || deviceRatio > 1.0) {
            throw new IllegalArgumentException("deviceRatio must be between 0.0 and 1.0, got: " + deviceRatio);
        }
        if (tailSamplingThresholdMs <= 0) {
            throw new IllegalArgumentException("tailSamplingThresholdMs must be positive, got: " + tailSamplingThresholdMs);
        }
    }

    // --- Getters and Setters ---

    public double getHeadRate() {
        return headRate;
    }

    public void setHeadRate(double headRate) {
        this.headRate = Math.max(0.0, Math.min(1.0, headRate));
    }

    public boolean isErrorAlways() {
        return errorAlways;
    }

    public void setErrorAlways(boolean errorAlways) {
        this.errorAlways = errorAlways;
    }

    public double getDeviceRatio() {
        return deviceRatio;
    }

    public void setDeviceRatio(double deviceRatio) {
        this.deviceRatio = Math.max(0.0, Math.min(1.0, deviceRatio));
    }

    public long getTailSamplingThresholdMs() {
        return tailSamplingThresholdMs;
    }

    public void setTailSamplingThresholdMs(long tailSamplingThresholdMs) {
        this.tailSamplingThresholdMs = tailSamplingThresholdMs;
    }

    public boolean isTailSamplingEnabled() {
        return tailSamplingEnabled;
    }

    public void setTailSamplingEnabled(boolean tailSamplingEnabled) {
        this.tailSamplingEnabled = tailSamplingEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SamplerConfig that = (SamplerConfig) o;
        return Double.compare(that.headRate, headRate) == 0
                && errorAlways == that.errorAlways
                && Double.compare(that.deviceRatio, deviceRatio) == 0
                && tailSamplingThresholdMs == that.tailSamplingThresholdMs
                && tailSamplingEnabled == that.tailSamplingEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(headRate, errorAlways, deviceRatio, tailSamplingThresholdMs, tailSamplingEnabled);
    }

    @Override
    public String toString() {
        return "SamplerConfig{"
                + "headRate=" + headRate
                + ", errorAlways=" + errorAlways
                + ", deviceRatio=" + deviceRatio
                + ", tailSamplingThresholdMs=" + tailSamplingThresholdMs
                + ", tailSamplingEnabled=" + tailSamplingEnabled
                + '}';
    }
}
