package com.bosch.iot.uok.agent;

import java.lang.instrument.Instrumentation;

/**
 * UOK Java Agent entry point.
 * Based on OpenTelemetry Java Agent bytecode enhancement for zero-intrusion instrumentation.
 * <p>
 * Usage: {@code java -javaagent:uok-agent.jar -jar your-app.jar}
 */
public class UokAgent {

    private static final String AGENT_ENABLED_KEY = "uok.agent.enabled";

    private UokAgent() {
        // Prevent instantiation
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
        System.out.println("[UOK] Agent starting with args: " + agentArgs);
        // Full OTel Agent integration will be implemented in Phase 2
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
}
