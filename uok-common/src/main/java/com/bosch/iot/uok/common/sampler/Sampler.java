package com.bosch.iot.uok.common.sampler;

/**
 * Base sampler interface for trace sampling decisions.
 * All sampler implementations must implement this interface.
 */
public interface Sampler {

    /**
     * Determine if a trace should be sampled.
     *
     * @param traceId the trace ID to evaluate
     * @return true if the trace should be sampled
     */
    boolean shouldSample(String traceId);

    /**
     * Get the description of this sampler.
     *
     * @return sampler description
     */
    String getDescription();
}
