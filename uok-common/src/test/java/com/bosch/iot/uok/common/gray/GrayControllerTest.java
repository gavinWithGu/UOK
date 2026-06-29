package com.bosch.iot.uok.common.gray;

import com.bosch.iot.uok.common.config.GrayConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GrayController}.
 */
class GrayControllerTest {

    private GrayController controller;

    @BeforeEach
    void setUp() {
        controller = new GrayController();
    }

    @Test
    @DisplayName("Should enable all by default")
    void shouldEnableAllByDefault() {
        assertThat(controller.isInstrumentationEnabled("any-service", "10.0.0.1", "any-tag")).isTrue();
    }

    @Test
    @DisplayName("Should enable based on service gray list")
    void shouldEnableBasedOnServiceList() {
        GrayConfig config = new GrayConfig();
        config.setServiceList(Arrays.asList("svc-a", "svc-b"));
        controller.updateConfig(config);

        assertThat(controller.isInstrumentationEnabled("svc-a", null, null)).isTrue();
        assertThat(controller.isInstrumentationEnabled("svc-c", null, null)).isFalse();
    }

    @Test
    @DisplayName("Should enable based on instance IP gray list")
    void shouldEnableBasedOnInstanceIpList() {
        GrayConfig config = new GrayConfig();
        config.setInstanceIps(Arrays.asList("10.0.0.1", "10.0.0.2"));
        controller.updateConfig(config);

        assertThat(controller.isInstrumentationEnabled("svc-a", "10.0.0.1", null)).isTrue();
        assertThat(controller.isInstrumentationEnabled("svc-a", "10.0.0.3", null)).isFalse();
    }

    @Test
    @DisplayName("Should enable based on instance tag gray list")
    void shouldEnableBasedOnInstanceTagList() {
        GrayConfig config = new GrayConfig();
        config.setInstanceTags(Arrays.asList("canary", "beta"));
        controller.updateConfig(config);

        assertThat(controller.isInstrumentationEnabled("svc-a", null, "canary")).isTrue();
        assertThat(controller.isInstrumentationEnabled("svc-a", null, "stable")).isFalse();
    }

    @Test
    @DisplayName("Should update config at runtime")
    void shouldUpdateConfigAtRuntime() {
        GrayConfig config1 = new GrayConfig();
        config1.addService("svc-a");
        controller.updateConfig(config1);

        assertThat(controller.isInstrumentationEnabled("svc-a", null, null)).isTrue();
        assertThat(controller.isInstrumentationEnabled("svc-b", null, null)).isFalse();

        GrayConfig config2 = new GrayConfig();
        config2.addService("svc-b");
        controller.updateConfig(config2);

        assertThat(controller.isInstrumentationEnabled("svc-b", null, null)).isTrue();
        assertThat(controller.isInstrumentationEnabled("svc-a", null, null)).isFalse();
    }

    @Test
    @DisplayName("Should check request tracing based on traffic ratio")
    void shouldCheckRequestTracing() {
        GrayConfig config = new GrayConfig();
        config.setTrafficRatio(1.0);
        controller.updateConfig(config);

        assertThat(controller.isRequestTraced(50)).isTrue();
    }

    @Test
    @DisplayName("Should handle null config update")
    void shouldHandleNullConfigUpdate() {
        controller.updateConfig(null);
        assertThat(controller.getConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should create with null config")
    void shouldCreateWithNullConfig() {
        GrayController ctrl = new GrayController(null);
        assertThat(ctrl.getConfig()).isNotNull();
    }

    @Test
    @DisplayName("Should get current config")
    void shouldGetCurrentConfig() {
        GrayConfig config = new GrayConfig();
        config.addService("svc-a");
        controller.updateConfig(config);

        assertThat(controller.getConfig().getServiceList()).contains("svc-a");
    }
}
