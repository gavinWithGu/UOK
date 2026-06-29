package com.bosch.iot.uok.common.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * UOK unified configuration model.
 * Holds all configuration items for the observability toolkit.
 * <p>
 * Configuration is loaded via {@link ConfigLoader} from system properties,
 * environment variables, or configuration files.
 */
public class UokConfig {

    private String serviceName = "unknown-service";
    private String bizDomain = "default";
    private String teamName = "default";
    private String env;
    private boolean agentEnabled = true;
    private boolean logEnabled = true;
    private boolean traceEnabled = true;
    private boolean metricsEnabled = true;
    private SamplerConfig samplerConfig = new SamplerConfig();
    private GrayConfig grayConfig = new GrayConfig();
    private int degradeCpuThreshold = 80;
    private int degradeLatencyIncrease = 10;

    public UokConfig() {
        this.env = detectEnvironment();
    }

    /**
     * Detect current environment from system properties or environment variables.
     *
     * @return detected environment (dev/test/prod)
     */
    private String detectEnvironment() {
        // Check system property first
        String env = System.getProperty("uok.env");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        // Check environment variable
        env = System.getenv("UOK_ENV");
        if (env != null && !env.isEmpty()) {
            return env;
        }
        // Check common environment indicators
        String springEnv = System.getProperty("spring.profiles.active");
        if (springEnv != null) {
            if (springEnv.contains("prod")) {
                return "prod";
            } else if (springEnv.contains("test")) {
                return "test";
            }
        }
        return "dev";
    }

    // --- Getters and Setters ---

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
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

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public boolean isAgentEnabled() {
        return agentEnabled;
    }

    public void setAgentEnabled(boolean agentEnabled) {
        this.agentEnabled = agentEnabled;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public SamplerConfig getSamplerConfig() {
        return samplerConfig;
    }

    public void setSamplerConfig(SamplerConfig samplerConfig) {
        this.samplerConfig = samplerConfig != null ? samplerConfig : new SamplerConfig();
    }

    public GrayConfig getGrayConfig() {
        return grayConfig;
    }

    public void setGrayConfig(GrayConfig grayConfig) {
        this.grayConfig = grayConfig != null ? grayConfig : new GrayConfig();
    }

    public int getDegradeCpuThreshold() {
        return degradeCpuThreshold;
    }

    public void setDegradeCpuThreshold(int degradeCpuThreshold) {
        this.degradeCpuThreshold = degradeCpuThreshold;
    }

    public int getDegradeLatencyIncrease() {
        return degradeLatencyIncrease;
    }

    public void setDegradeLatencyIncrease(int degradeLatencyIncrease) {
        this.degradeLatencyIncrease = degradeLatencyIncrease;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UokConfig that = (UokConfig) o;
        return agentEnabled == that.agentEnabled
                && logEnabled == that.logEnabled
                && traceEnabled == that.traceEnabled
                && metricsEnabled == that.metricsEnabled
                && degradeCpuThreshold == that.degradeCpuThreshold
                && degradeLatencyIncrease == that.degradeLatencyIncrease
                && Objects.equals(serviceName, that.serviceName)
                && Objects.equals(bizDomain, that.bizDomain)
                && Objects.equals(teamName, that.teamName)
                && Objects.equals(env, that.env)
                && Objects.equals(samplerConfig, that.samplerConfig)
                && Objects.equals(grayConfig, that.grayConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, bizDomain, teamName, env, agentEnabled,
                logEnabled, traceEnabled, metricsEnabled, samplerConfig,
                grayConfig, degradeCpuThreshold, degradeLatencyIncrease);
    }

    @Override
    public String toString() {
        return "UokConfig{"
                + "serviceName='" + serviceName + '\''
                + ", bizDomain='" + bizDomain + '\''
                + ", teamName='" + teamName + '\''
                + ", env='" + env + '\''
                + ", agentEnabled=" + agentEnabled
                + ", logEnabled=" + logEnabled
                + ", traceEnabled=" + traceEnabled
                + ", metricsEnabled=" + metricsEnabled
                + ", samplerConfig=" + samplerConfig
                + ", grayConfig=" + grayConfig
                + ", degradeCpuThreshold=" + degradeCpuThreshold
                + ", degradeLatencyIncrease=" + degradeLatencyIncrease
                + '}';
    }

    /**
     * Builder for UokConfig.
     */
    public static class Builder {
        private final UokConfig config = new UokConfig();

        public Builder serviceName(String serviceName) {
            config.setServiceName(serviceName);
            return this;
        }

        public Builder bizDomain(String bizDomain) {
            config.setBizDomain(bizDomain);
            return this;
        }

        public Builder teamName(String teamName) {
            config.setTeamName(teamName);
            return this;
        }

        public Builder env(String env) {
            config.setEnv(env);
            return this;
        }

        public Builder agentEnabled(boolean enabled) {
            config.setAgentEnabled(enabled);
            return this;
        }

        public Builder logEnabled(boolean enabled) {
            config.setLogEnabled(enabled);
            return this;
        }

        public Builder traceEnabled(boolean enabled) {
            config.setTraceEnabled(enabled);
            return this;
        }

        public Builder metricsEnabled(boolean enabled) {
            config.setMetricsEnabled(enabled);
            return this;
        }

        public Builder samplerConfig(SamplerConfig samplerConfig) {
            config.setSamplerConfig(samplerConfig);
            return this;
        }

        public Builder grayConfig(GrayConfig grayConfig) {
            config.setGrayConfig(grayConfig);
            return this;
        }

        public Builder degradeCpuThreshold(int threshold) {
            config.setDegradeCpuThreshold(threshold);
            return this;
        }

        public Builder degradeLatencyIncrease(int increase) {
            config.setDegradeLatencyIncrease(increase);
            return this;
        }

        public UokConfig build() {
            return config;
        }
    }
}
