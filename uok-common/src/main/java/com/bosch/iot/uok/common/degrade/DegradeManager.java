package com.bosch.iot.uok.common.degrade;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Degradation manager for automatic and manual circuit breaking.
 * Monitors CPU usage and latency increase, automatically degrades
 * observability instrumentation when thresholds are exceeded.
 * <p>
 * Supports dual degradation mechanism:
 * 1. Manual: explicitly enable/disable via configuration
 * 2. Automatic: auto-degrade when CPU or latency thresholds are exceeded
 */
public class DegradeManager {

    /**
     * Degradation level enumeration.
     */
    public enum DegradeLevel {
        /** Full observability active */
        FULL(0),
        /** Reduced: only log injection, no tracing */
        REDUCED(1),
        /** Minimal: only basic log output */
        MINIMAL(2),
        /** Disabled: all instrumentation off */
        DISABLED(3);

        private final int level;

        DegradeLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }

        public boolean isAtLeast(DegradeLevel other) {
            return this.level <= other.level;
        }
    }

    private final int cpuThreshold;
    private final int latencyIncreaseThreshold;
    private final AtomicBoolean manualDegraded = new AtomicBoolean(false);
    private final AtomicBoolean autoDegraded = new AtomicBoolean(false);
    private volatile DegradeLevel currentLevel = DegradeLevel.FULL;
    private final AtomicLong lastCpuCheckTime = new AtomicLong(0);
    private volatile double lastCpuUsage = 0.0;
    private volatile double lastLatencyIncrease = 0.0;

    private static final long CPU_CHECK_INTERVAL_MS = 5000;

    /**
     * Create a degradation manager.
     *
     * @param cpuThreshold           CPU usage threshold percentage (0-100)
     * @param latencyIncreaseThreshold latency increase threshold percentage (0-100)
     */
    public DegradeManager(int cpuThreshold, int latencyIncreaseThreshold) {
        this.cpuThreshold = Math.max(0, Math.min(100, cpuThreshold));
        this.latencyIncreaseThreshold = Math.max(0, Math.min(100, latencyIncreaseThreshold));
    }

    /**
     * Create a degradation manager with default thresholds.
     */
    public DegradeManager() {
        this(80, 10);
    }

    /**
     * Check if observability should be active at the given level.
     *
     * @param requiredLevel the required degradation level
     * @return true if observability at the required level is active
     */
    public boolean isActive(DegradeLevel requiredLevel) {
        return getCurrentLevel().isAtLeast(requiredLevel);
    }

    /**
     * Check if tracing is currently active.
     *
     * @return true if tracing is active
     */
    public boolean isTracingActive() {
        return isActive(DegradeLevel.REDUCED);
    }

    /**
     * Check if log injection is currently active.
     *
     * @return true if log injection is active
     */
    public boolean isLogInjectionActive() {
        return isActive(DegradeLevel.MINIMAL);
    }

    /**
     * Get the current degradation level.
     *
     * @return the current degradation level
     */
    public DegradeLevel getCurrentLevel() {
        if (manualDegraded.get()) {
            return DegradeLevel.DISABLED;
        }
        if (autoDegraded.get()) {
            return DegradeLevel.REDUCED;
        }
        return DegradeLevel.FULL;
    }

    /**
     * Perform a health check and update auto-degradation status.
     * Should be called periodically.
     *
     * @param currentCpuUsage       current CPU usage percentage
     * @param currentLatencyIncrease current latency increase percentage
     */
    public void checkAndDegrade(double currentCpuUsage, double currentLatencyIncrease) {
        this.lastCpuUsage = currentCpuUsage;
        this.lastLatencyIncrease = currentLatencyIncrease;

        boolean shouldAutoDegrade = currentCpuUsage > cpuThreshold
                || currentLatencyIncrease > latencyIncreaseThreshold;

        if (shouldAutoDegrade && !autoDegraded.get()) {
            autoDegraded.compareAndSet(false, true);
        } else if (!shouldAutoDegrade && autoDegraded.get()) {
            // Auto-recover when conditions are back to normal
            autoDegraded.compareAndSet(true, false);
        }
    }

    /**
     * Perform a health check using system CPU metrics.
     */
    public void checkSystemHealth() {
        long now = System.currentTimeMillis();
        long lastCheck = lastCpuCheckTime.get();

        if (now - lastCheck < CPU_CHECK_INTERVAL_MS) {
            return;
        }

        if (lastCpuCheckTime.compareAndSet(lastCheck, now)) {
            double cpuUsage = getCpuUsage();
            checkAndDegrade(cpuUsage, lastLatencyIncrease);
        }
    }

    /**
     * Manually enable degradation (disable all instrumentation).
     */
    public void manualDegrade() {
        manualDegraded.set(true);
    }

    /**
     * Manually recover from degradation (re-enable all instrumentation).
     */
    public void manualRecover() {
        manualDegraded.set(false);
    }

    /**
     * Get current CPU usage.
     *
     * @return CPU usage as a percentage (0-100)
     */
    private double getCpuUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            double cpuLoad = sunOsBean.getProcessCpuLoad() * 100.0;
            return Math.max(0.0, cpuLoad);
        }
        // Fallback: use system load average
        double loadAverage = osBean.getSystemLoadAverage();
        int processors = osBean.getAvailableProcessors();
        if (loadAverage >= 0 && processors > 0) {
            return (loadAverage / processors) * 100.0;
        }
        return 0.0;
    }

    // --- Getters ---

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public int getLatencyIncreaseThreshold() {
        return latencyIncreaseThreshold;
    }

    public boolean isManualDegraded() {
        return manualDegraded.get();
    }

    public boolean isAutoDegraded() {
        return autoDegraded.get();
    }

    public double getLastCpuUsage() {
        return lastCpuUsage;
    }

    public double getLastLatencyIncrease() {
        return lastLatencyIncrease;
    }
}
