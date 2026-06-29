package com.bosch.iot.uok.agent.config;

import com.bosch.iot.uok.common.config.ConfigLoader;
import com.bosch.iot.uok.common.config.UokConfig;

/**
 * Agent-specific configuration loader.
 * Extends the common ConfigLoader with agent-specific properties.
 */
public class AgentConfig {

    private AgentConfig() {
    }

    /**
     * Load agent configuration.
     * First loads common UokConfig, then applies agent-specific overrides.
     *
     * @return loaded UokConfig with agent overrides applied
     */
    public static UokConfig load() {
        UokConfig config = ConfigLoader.load();

        // Agent-specific overrides from system properties
        String otlpEndpoint = System.getProperty("uok.otlp.endpoint");
        if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
            System.setProperty("otel.exporter.otlp.endpoint", otlpEndpoint);
        }

        String prometheusPort = System.getProperty("uok.prometheus.port");
        if (prometheusPort != null && !prometheusPort.isEmpty()) {
            System.setProperty("otel.exporter.prometheus.port", prometheusPort);
        }

        // Set OTel resource attributes from UOK config
        System.setProperty("otel.resource.attributes",
                "service.name=" + config.getServiceName()
                        + ",service.version=1.0.0"
                        + ",deployment.environment=" + config.getEnv());

        // Set OTel sampler based on UOK config
        if (config.getSamplerConfig().getHeadRate() < 1.0) {
            System.setProperty("otel.traces.sampler", "traceidratio");
            System.setProperty("otel.traces.sampler.arg",
                    String.valueOf(config.getSamplerConfig().getHeadRate()));
        }

        return config;
    }
}
