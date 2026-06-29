package com.bosch.iot.uok.common.degrade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DegradeManager}.
 */
class DegradeManagerTest {

    private DegradeManager manager;

    @BeforeEach
    void setUp() {
        manager = new DegradeManager(80, 10);
    }

    @Test
    @DisplayName("Should start in FULL level")
    void shouldStartInFullLevel() {
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.FULL);
        assertThat(manager.isTracingActive()).isTrue();
        assertThat(manager.isLogInjectionActive()).isTrue();
    }

    @Test
    @DisplayName("Should auto-degrade when CPU exceeds threshold")
    void shouldAutoDegradeWhenCpuExceedsThreshold() {
        manager.checkAndDegrade(90.0, 5.0);

        assertThat(manager.isAutoDegraded()).isTrue();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.REDUCED);
        assertThat(manager.isTracingActive()).isTrue();
    }

    @Test
    @DisplayName("Should auto-degrade when latency increase exceeds threshold")
    void shouldAutoDegradeWhenLatencyExceedsThreshold() {
        manager.checkAndDegrade(50.0, 15.0);

        assertThat(manager.isAutoDegraded()).isTrue();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.REDUCED);
    }

    @Test
    @DisplayName("Should auto-recover when conditions normalize")
    void shouldAutoRecoverWhenConditionsNormalize() {
        manager.checkAndDegrade(90.0, 5.0);
        assertThat(manager.isAutoDegraded()).isTrue();

        manager.checkAndDegrade(50.0, 5.0);
        assertThat(manager.isAutoDegraded()).isFalse();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.FULL);
    }

    @Test
    @DisplayName("Should manually degrade")
    void shouldManuallyDegrade() {
        manager.manualDegrade();

        assertThat(manager.isManualDegraded()).isTrue();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.DISABLED);
        assertThat(manager.isTracingActive()).isFalse();
        assertThat(manager.isLogInjectionActive()).isFalse();
    }

    @Test
    @DisplayName("Should manually recover")
    void shouldManuallyRecover() {
        manager.manualDegrade();
        assertThat(manager.isManualDegraded()).isTrue();

        manager.manualRecover();
        assertThat(manager.isManualDegraded()).isFalse();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.FULL);
    }

    @Test
    @DisplayName("Manual degrade should override auto recover")
    void manualDegradeShouldOverrideAutoRecover() {
        manager.manualDegrade();
        manager.checkAndDegrade(30.0, 5.0); // Normal conditions

        // Manual degrade should still be active
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.DISABLED);
    }

    @Test
    @DisplayName("Should return correct thresholds")
    void shouldReturnCorrectThresholds() {
        assertThat(manager.getCpuThreshold()).isEqualTo(80);
        assertThat(manager.getLatencyIncreaseThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should track last CPU usage and latency increase")
    void shouldTrackLastCpuAndLatency() {
        manager.checkAndDegrade(75.0, 8.0);
        assertThat(manager.getLastCpuUsage()).isEqualTo(75.0);
        assertThat(manager.getLastLatencyIncrease()).isEqualTo(8.0);
    }

    @Test
    @DisplayName("Should create with default constructor")
    void shouldCreateWithDefaultConstructor() {
        DegradeManager defaultManager = new DegradeManager();
        assertThat(defaultManager.getCpuThreshold()).isEqualTo(80);
        assertThat(defaultManager.getLatencyIncreaseThreshold()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should check system health without error")
    void shouldCheckSystemHealth() {
        // This test just verifies no exception is thrown
        manager.checkSystemHealth();
        // CPU usage may be 0 in test environment
    }

    @Test
    @DisplayName("DegradeLevel should compare correctly")
    void degradeLevelShouldCompareCorrectly() {
        assertThat(DegradeManager.DegradeLevel.FULL.isAtLeast(DegradeManager.DegradeLevel.REDUCED)).isTrue();
        assertThat(DegradeManager.DegradeLevel.REDUCED.isAtLeast(DegradeManager.DegradeLevel.FULL)).isFalse();
        assertThat(DegradeManager.DegradeLevel.DISABLED.isAtLeast(DegradeManager.DegradeLevel.MINIMAL)).isFalse();
    }

    @Test
    @DisplayName("Should not degrade when within thresholds")
    void shouldNotDegradeWhenWithinThresholds() {
        manager.checkAndDegrade(70.0, 5.0);
        assertThat(manager.isAutoDegraded()).isFalse();
        assertThat(manager.getCurrentLevel()).isEqualTo(DegradeManager.DegradeLevel.FULL);
    }

    @Test
    @DisplayName("isActive should check against required level")
    void isActiveShouldCheckAgainstRequiredLevel() {
        assertThat(manager.isActive(DegradeManager.DegradeLevel.FULL)).isTrue();
        assertThat(manager.isActive(DegradeManager.DegradeLevel.REDUCED)).isTrue();
        assertThat(manager.isActive(DegradeManager.DegradeLevel.MINIMAL)).isTrue();

        manager.manualDegrade();
        assertThat(manager.isActive(DegradeManager.DegradeLevel.FULL)).isFalse();
    }
}
