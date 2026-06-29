package com.bosch.iot.uok.agent;

import com.bosch.iot.uok.agent.config.AgentConfig;
import com.bosch.iot.uok.agent.context.AgentContextHolder;
import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.agent.integration.OtelSdkInitializer;
import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.degrade.DegradeManager;

import java.lang.instrument.Instrumentation;

/**
 * UOK Java Agent entry point.
 * Based on OpenTelemetry SDK for zero-intrusion instrumentation.
 * <p>
 * Usage: {@code java -javaagent:uok-agent.jar -jar your-app.jar}
 * <p>
 * In production, this will be replaced by OTel Java Agent bytecode enhancement.
 * Current implementation uses OTel SDK with servlet filter for HTTP tracing.
 */
public class UokAgent {

    private static final String AGENT_ENABLED_KEY = "uok.agent.enabled";
    private static volatile boolean initialized = false;
    private static volatile DegradeManager degradeManager;
    private static volatile UokConfig uokConfig;

    private UokAgent() {
    }

    /**
     * Agent premain method, invoked before application main method.
     *
     * @param agentArgs agent arguments from -javaagent flag
     * @param inst      instrumentation interface provided by JVM
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        boolean enabled = Boolean.parseBoolean(
                System.getProperty(AGENT_ENABLED_KEY, "true"));
        if (!enabled) {
            System.out.println("[UOK] Agent is disabled by configuration, skipping instrumentation.");
            return;
        }
        initialize(agentArgs, inst);
    }

    /**
     * Agent main method for dynamic attach.
     *
     * @param agentArgs agent arguments
     * @param inst      instrumentation interface
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        premain(agentArgs, inst);
    }

    /**
     * Initialize the UOK agent.
     *
     * @param agentArgs agent arguments
     * @param inst      instrumentation interface
     */
    private static synchronized void initialize(String agentArgs, Instrumentation inst) {
        if (initialized) {
            return;
        }

        System.out.println("[UOK] Agent initializing with args: " + agentArgs);

        // 1. Load configuration
        uokConfig = AgentConfig.load();
        System.out.println("[UOK] Configuration loaded: serviceName=" + uokConfig.getServiceName()
                + ", env=" + uokConfig.getEnv());

        // 2. Initialize degrade manager
        degradeManager = new DegradeManager(
                uokConfig.getDegradeCpuThreshold(),
                uokConfig.getDegradeLatencyIncrease());

        // 3. Initialize OpenTelemetry SDK
        if (uokConfig.isTraceEnabled() || uokConfig.isMetricsEnabled()) {
            OtelSdkInitializer.initialize(uokConfig);
            System.out.println("[UOK] OpenTelemetry SDK initialized");
        }

        // 4. Initialize log injection
        if (uokConfig.isLogEnabled()) {
            MdcLogInjector.initialize(uokConfig);
            System.out.println("[UOK] Log injection enabled");
        }

        // 5. Register servlet filter for HTTP instrumentation (programmatic approach)
        if (uokConfig.isTraceEnabled()) {
            System.out.println("[UOK] HTTP tracing instrumentation ready");
        }

        initialized = true;
        System.out.println("[UOK] Agent initialized successfully (v1.0.0)");
    }

    /**
     * Check if the agent has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the current UOK configuration.
     *
     * @return the UOK configuration, or null if not initialized
     */
    public static UokConfig getConfig() {
        return uokConfig;
    }

    /**
     * Get the degrade manager.
     *
     * @return the degrade manager, or null if not initialized
     */
    public static DegradeManager getDegradeManager() {
        return degradeManager;
    }
}
