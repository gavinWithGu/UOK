package com.bosch.iot.uok.common.context;

import java.util.Objects;

/**
 * Trace context model representing a distributed trace span.
 * Contains the standard W3C Trace Context fields plus UOK extensions.
 */
public class TraceContext {

    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String serviceName;
    private String env;
    private String bizDomain;
    private String teamName;
    private boolean sampled = true;

    public TraceContext() {
    }

    /**
     * Create a root trace context with no parent.
     *
     * @param traceId     the trace ID
     * @param spanId      the span ID
     * @param serviceName the service name
     */
    public TraceContext(String traceId, String spanId, String serviceName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = null;
        this.serviceName = serviceName;
    }

    /**
     * Create a child trace context from a parent context.
     *
     * @param parent       the parent trace context
     * @param childSpanId  the new child span ID
     * @param serviceName  the service name for the child span
     * @return a new TraceContext representing the child span
     */
    public static TraceContext createChild(TraceContext parent, String childSpanId, String serviceName) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent trace context cannot be null");
        }
        TraceContext child = new TraceContext();
        child.setTraceId(parent.getTraceId());
        child.setSpanId(childSpanId);
        child.setParentSpanId(parent.getSpanId());
        child.setServiceName(serviceName);
        child.setEnv(parent.getEnv());
        child.setBizDomain(parent.getBizDomain());
        child.setTeamName(parent.getTeamName());
        child.setSampled(parent.isSampled());
        return child;
    }

    /**
     * Check if this is a root span (no parent).
     *
     * @return true if this is a root span
     */
    public boolean isRoot() {
        return parentSpanId == null || parentSpanId.isEmpty();
    }

    /**
     * Validate the trace context fields.
     *
     * @return true if the context has valid trace and span IDs
     */
    public boolean isValid() {
        return traceId != null && !traceId.isEmpty()
                && spanId != null && !spanId.isEmpty();
    }

    // --- Getters and Setters ---

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSpanId() {
        return spanId;
    }

    public void setSpanId(String spanId) {
        this.spanId = spanId;
    }

    public String getParentSpanId() {
        return parentSpanId;
    }

    public void setParentSpanId(String parentSpanId) {
        this.parentSpanId = parentSpanId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getBizDomain() {
        return bizDomain;
    }

    public void setBizDomain(String bizDomain) {
        this.bizDomain = bizDomain;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public boolean isSampled() {
        return sampled;
    }

    public void setSampled(boolean sampled) {
        this.sampled = sampled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TraceContext that = (TraceContext) o;
        return sampled == that.sampled
                && Objects.equals(traceId, that.traceId)
                && Objects.equals(spanId, that.spanId)
                && Objects.equals(parentSpanId, that.parentSpanId)
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(env, that.env)
                && Objects.equals(bizDomain, that.bizDomain)
                && Objects.equals(teamName, that.teamName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(traceId, spanId, parentSpanId, serviceName, env, bizDomain, teamName, sampled);
    }

    @Override
    public String toString() {
        return "TraceContext{"
                + "traceId='" + traceId + '\''
                + ", spanId='" + spanId + '\''
                + ", parentSpanId='" + parentSpanId + '\''
                + ", serviceName='" + serviceName + '\''
                + ", env='" + env + '\''
                + ", bizDomain='" + bizDomain + '\''
                + ", teamName='" + teamName + '\''
                + ", sampled=" + sampled
                + '}';
    }
}
