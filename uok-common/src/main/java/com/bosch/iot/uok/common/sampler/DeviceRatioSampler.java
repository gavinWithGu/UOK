package com.bosch.iot.uok.common.sampler;

/**
 * Device ratio sampler - samples traces based on device ID hash.
 * Designed for IoT scenarios where traces from a subset of devices
 * should be sampled to reduce storage costs while maintaining
 * troubleshooting capability.
 */
public class DeviceRatioSampler implements Sampler {

    private final double ratio;

    /**
     * Create a device ratio sampler with the specified ratio.
     *
     * @param ratio the sampling ratio between 0.0 and 1.0
     */
    public DeviceRatioSampler(double ratio) {
        if (ratio < 0.0 || ratio > 1.0) {
            throw new IllegalArgumentException("Device ratio must be between 0.0 and 1.0, got: " + ratio);
        }
        this.ratio = ratio;
    }

    /**
     * Create a device ratio sampler with 100% sampling.
     */
    public DeviceRatioSampler() {
        this(1.0);
    }

    @Override
    public boolean shouldSample(String traceId) {
        // For device ratio sampling, we use deviceId, not traceId
        return ratio >= 1.0;
    }

    /**
     * Determine if a device's traces should be sampled.
     * Uses consistent hashing on the device ID to ensure
     * the same device is always sampled or not sampled.
     *
     * @param deviceId the device ID to evaluate
     * @return true if the device's traces should be sampled
     */
    public boolean shouldSampleDevice(String deviceId) {
        if (ratio >= 1.0) {
            return true;
        }
        if (ratio <= 0.0) {
            return false;
        }
        if (deviceId == null || deviceId.isEmpty()) {
            return true;
        }
        int hash = Math.abs(deviceId.hashCode());
        return (hash % 100) < (ratio * 100);
    }

    @Override
    public String getDescription() {
        return "DeviceRatioSampler{ratio=" + ratio + '}';
    }

    public double getRatio() {
        return ratio;
    }
}
