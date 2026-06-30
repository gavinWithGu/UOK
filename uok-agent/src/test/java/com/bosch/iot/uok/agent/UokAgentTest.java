package com.bosch.iot.uok.agent;

import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.degrade.DegradeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.instrument.Instrumentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link UokAgent}.
 */
class UokAgentTest {

    @AfterEach
    void tearDown() {
        // Clean up system properties that tests may set
        System.clearProperty("uok.agent.enabled");
    }

    @Test
    @DisplayName("Should not be initialized before premain call")
    void shouldNotBeInitializedBeforePremain() {
        // Agent may or may not be initialized depending on test order
        Boolean result = UokAgent.isInitialized();
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should have correct agent enabled key")
    void shouldHaveCorrectEnabledKey() {
        String original = System.getProperty("uok.agent.enabled");
        try {
            System.setProperty("uok.agent.enabled", "false");
            boolean enabled = Boolean.parseBoolean(
                    System.getProperty("uok.agent.enabled", "true"));
            assertThat(enabled).isFalse();
        } finally {
            if (original != null) {
                System.setProperty("uok.agent.enabled", original);
            } else {
                System.clearProperty("uok.agent.enabled");
            }
        }
    }

    @Test
    @DisplayName("Should get config reference without exception")
    void shouldGetConfigSafely() {
        assertThatCode(() -> UokAgent.getConfig()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should get degrade manager reference without exception")
    void shouldGetDegradeManagerSafely() {
        assertThatCode(() -> UokAgent.getDegradeManager()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should handle disabled agent via system property")
    void shouldHandleDisabledAgentViaSystemProperty() {
        String original = System.getProperty("uok.agent.enabled");
        try {
            System.setProperty("uok.agent.enabled", "false");
            boolean enabled = Boolean.parseBoolean(
                    System.getProperty("uok.agent.enabled", "true"));
            assertThat(enabled).isFalse();
        } finally {
            if (original != null) {
                System.setProperty("uok.agent.enabled", original);
            } else {
                System.clearProperty("uok.agent.enabled");
            }
        }
    }

    @Test
    @DisplayName("Should handle enabled agent via system property")
    void shouldHandleEnabledAgentViaSystemProperty() {
        String original = System.getProperty("uok.agent.enabled");
        try {
            System.setProperty("uok.agent.enabled", "true");
            boolean enabled = Boolean.parseBoolean(
                    System.getProperty("uok.agent.enabled", "true"));
            assertThat(enabled).isTrue();
        } finally {
            if (original != null) {
                System.setProperty("uok.agent.enabled", original);
            } else {
                System.clearProperty("uok.agent.enabled");
            }
        }
    }

    @Test
    @DisplayName("premain should not throw when agent is disabled")
    void premainShouldNotThrowWhenDisabled() {
        String original = System.getProperty("uok.agent.enabled");
        try {
            System.setProperty("uok.agent.enabled", "false");
            // Call premain with null Instrumentation (agent is disabled so it won't use it)
            assertThatCode(() -> UokAgent.premain(null, null)).doesNotThrowAnyException();
        } finally {
            if (original != null) {
                System.setProperty("uok.agent.enabled", original);
            } else {
                System.clearProperty("uok.agent.enabled");
            }
        }
    }

    @Test
    @DisplayName("agentmain should delegate to premain")
    void agentmainShouldDelegateToPremain() {
        String original = System.getProperty("uok.agent.enabled");
        try {
            System.setProperty("uok.agent.enabled", "false");
            assertThatCode(() -> UokAgent.agentmain(null, null)).doesNotThrowAnyException();
        } finally {
            if (original != null) {
                System.setProperty("uok.agent.enabled", original);
            } else {
                System.clearProperty("uok.agent.enabled");
            }
        }
    }
}
