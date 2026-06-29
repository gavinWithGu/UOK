package com.bosch.iot.uok.common.sampler;

/**
 * Tail sampler - makes sampling decisions based on trace characteristics
 * after the trace has completed (tail-based sampling).
 * <p>
 * Supports sampling based on:
 * - Duration threshold: traces exceeding the threshold are always sampled
 * - Error presence: traces with errors are always sampled
 * - Custom attributes: traces matching certain attribute criteria are sampled
 */
public class TailSampler implements Sampler {

    private final boolean enabled;
    private final long durationThresholdMs;
    private final boolean sampleErrors;

    /**
     * Create a tail sampler.
     *
     * @param enabled             whether tail sampling is enabled
     * @param durationThresholdMs duration threshold in milliseconds
     * @param sampleErrors        whether to always sample error traces
     */
    public TailSampler(boolean enabled, long durationThresholdMs, boolean sampleErrors) {
        this.enabled = enabled;
        this.durationThresholdMs = durationThresholdMs;
        this.sampleErrors = sampleErrors;
    }

    /**
     * Create a tail sampler with default settings.
     */
    public TailSampler() {
        this(false, 1000, true);
    }

    @Override
    public boolean shouldSample(String traceId) {
        // Base tail sampling always returns false;
        // actual sampling decision is made in shouldSampleTail()
        return false;
    }

    /**
     * Determine if a completed trace should be sampled based on its characteristics.
     *
     * @param durationMs  the trace duration in milliseconds
     * @param hasError    whether the trace has an error
     * @return true if the trace should be sampled
     */
    public boolean shouldSampleTail(long durationMs, boolean hasError) {
        if (!enabled) {
            return false;
        }
        // Always sample error traces if configured
        if (sampleErrors && hasError) {
            return true;
        }
        // Always sample slow traces
        if (durationMs >= durationThresholdMs) {
            return true;
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "TailSampler{enabled=" + enabled
                + ", durationThresholdMs=" + durationThresholdMs
                + ", sampleErrors=" + sampleErrors + '}';
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getDurationThresholdMs() {
        return durationThresholdMs;
    }

    public boolean isSampleErrors() {
        return sampleErrors;
    }
}
