package com.bosch.iot.uok.common.gray;

import com.bosch.iot.uok.common.config.GrayConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Gray release controller.
 * Determines whether observability instrumentation should be active
 * for a given service/instance based on gray release rules.
 */
public class GrayController {

    private volatile GrayConfig config;

    /**
     * Create a gray controller with the specified configuration.
     *
     * @param config the gray release configuration
     */
    public GrayController(GrayConfig config) {
        this.config = config != null ? config : new GrayConfig();
    }

    /**
     * Create a gray controller with default configuration (all enabled).
     */
    public GrayController() {
        this.config = new GrayConfig();
    }

    /**
     * Check if observability should be enabled for a given service and instance.
     *
     * @param serviceName the service name
     * @param instanceIp  the instance IP address
     * @param instanceTag the instance tag (optional)
     * @return true if instrumentation should be active
     */
    public boolean isInstrumentationEnabled(String serviceName, String instanceIp, String instanceTag) {
        if (config == null) {
            return true;
        }

        // Check service gray list
        if (!config.isServiceEnabled(serviceName)) {
            return false;
        }

        // Check instance IP gray list
        if (instanceIp != null && !config.isInstanceIpEnabled(instanceIp)) {
            return false;
        }

        // Check instance tag gray list
        if (instanceTag != null && !config.isInstanceTagEnabled(instanceTag)) {
            return false;
        }

        // Check instance ratio
        String instanceId = resolveInstanceId(instanceIp);
        if (!config.isInstanceRatioEnabled(instanceId.hashCode())) {
            return false;
        }

        return true;
    }

    /**
     * Check if a specific request should be traced based on traffic ratio.
     *
     * @param requestHash hash of the request identifier
     * @return true if the request should be traced
     */
    public boolean isRequestTraced(int requestHash) {
        if (config == null) {
            return true;
        }
        return config.isTrafficRatioEnabled(requestHash);
    }

    /**
     * Update the gray release configuration at runtime.
     *
     * @param newConfig the new gray configuration
     */
    public void updateConfig(GrayConfig newConfig) {
        this.config = newConfig != null ? newConfig : new GrayConfig();
    }

    /**
     * Get the current gray configuration.
     *
     * @return the current configuration
     */
    public GrayConfig getConfig() {
        return config;
    }

    /**
     * Resolve the instance identifier from IP or hostname.
     */
    private String resolveInstanceId(String instanceIp) {
        if (instanceIp != null && !instanceIp.isEmpty()) {
            return instanceIp;
        }
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
