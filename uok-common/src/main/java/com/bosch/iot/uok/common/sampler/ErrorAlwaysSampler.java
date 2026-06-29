package com.bosch.iot.uok.common.sampler;

/**
 * Error always sampler - forces 100% sampling for error/exception traces.
 * When enabled, any trace that encounters an error is always sampled,
 * regardless of head sampling decision.
 */
public class ErrorAlwaysSampler implements Sampler {

    private final boolean enabled;

    /**
     * Create an error always sampler.
     *
     * @param enabled whether error always sampling is enabled
     */
    public ErrorAlwaysSampler(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Create an error always sampler with default enabled state.
     */
    public ErrorAlwaysSampler() {
        this(true);
    }

    @Override
    public boolean shouldSample(String traceId) {
        return enabled;
    }

    /**
     * Determine if an error trace should be sampled.
     * This always returns true when enabled, regardless of traceId.
     *
     * @param traceId the trace ID (ignored for error sampling)
     * @param hasError whether the trace has an error
     * @return true if the error trace should be sampled
     */
    public boolean shouldSampleError(String traceId, boolean hasError) {
        if (!enabled) {
            return false;
        }
        return hasError;
    }

    @Override
    public String getDescription() {
        return "ErrorAlwaysSampler{enabled=" + enabled + '}';
    }

    public boolean isEnabled() {
        return enabled;
    }
}
