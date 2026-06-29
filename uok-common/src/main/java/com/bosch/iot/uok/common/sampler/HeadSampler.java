package com.bosch.iot.uok.common.sampler;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Head sampler - makes sampling decisions at the beginning of a trace.
 * Uses a configurable rate to determine if a trace should be sampled.
 */
public class HeadSampler implements Sampler {

    private final double rate;

    /**
     * Create a head sampler with the specified rate.
     *
     * @param rate sampling rate between 0.0 and 1.0
     */
    public HeadSampler(double rate) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("Sampler rate must be between 0.0 and 1.0, got: " + rate);
        }
        this.rate = rate;
    }

    /**
     * Create a head sampler with 100% sampling rate.
     */
    public HeadSampler() {
        this(1.0);
    }

    @Override
    public boolean shouldSample(String traceId) {
        if (rate >= 1.0) {
            return true;
        }
        if (rate <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < rate;
    }

    @Override
    public String getDescription() {
        return "HeadSampler{rate=" + rate + '}';
    }

    public double getRate() {
        return rate;
    }
}
