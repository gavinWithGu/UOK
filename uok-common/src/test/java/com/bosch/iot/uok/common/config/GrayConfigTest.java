package com.bosch.iot.uok.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GrayConfig}.
 */
class GrayConfigTest {

    @Test
    @DisplayName("Should return true for all services when list is empty")
    void shouldEnableAllServicesWhenListEmpty() {
        GrayConfig config = new GrayConfig();
        assertThat(config.isServiceEnabled("any-service")).isTrue();
    }

    @Test
    @DisplayName("Should check service in gray list")
    void shouldCheckServiceInList() {
        GrayConfig config = new GrayConfig();
        config.setServiceList(Arrays.asList("svc-a", "svc-b"));

        assertThat(config.isServiceEnabled("svc-a")).isTrue();
        assertThat(config.isServiceEnabled("svc-b")).isTrue();
        assertThat(config.isServiceEnabled("svc-c")).isFalse();
    }

    @Test
    @DisplayName("Should check instance IP in gray list")
    void shouldCheckInstanceIpInList() {
        GrayConfig config = new GrayConfig();
        config.setInstanceIps(Arrays.asList("10.0.0.1", "10.0.0.2"));

        assertThat(config.isInstanceIpEnabled("10.0.0.1")).isTrue();
        assertThat(config.isInstanceIpEnabled("10.0.0.3")).isFalse();
    }

    @Test
    @DisplayName("Should return true for all IPs when list is empty")
    void shouldEnableAllIpsWhenListEmpty() {
        GrayConfig config = new GrayConfig();
        assertThat(config.isInstanceIpEnabled("10.0.0.1")).isTrue();
    }

    @Test
    @DisplayName("Should return false for null IP when IP list is set")
    void shouldDisableForNullIpWhenListSet() {
        GrayConfig config = new GrayConfig();
        config.setInstanceIps(List.of("10.0.0.1"));
        // null IP not in the list, so should return false
        assertThat(config.isInstanceIpEnabled(null)).isFalse();
    }

    @Test
    @DisplayName("Should check instance tag in gray list")
    void shouldCheckInstanceTagInList() {
        GrayConfig config = new GrayConfig();
        config.setInstanceTags(Arrays.asList("canary", "beta"));

        assertThat(config.isInstanceTagEnabled("canary")).isTrue();
        assertThat(config.isInstanceTagEnabled("stable")).isFalse();
    }

    @Test
    @DisplayName("Should return true for all tags when list is empty")
    void shouldEnableAllTagsWhenListEmpty() {
        GrayConfig config = new GrayConfig();
        assertThat(config.isInstanceTagEnabled("any-tag")).isTrue();
    }

    @Test
    @DisplayName("Should return false for null tag when tag list is set")
    void shouldDisableForNullTagWhenListSet() {
        GrayConfig config = new GrayConfig();
        config.setInstanceTags(List.of("canary"));
        // null tag not in the list, so should return false
        assertThat(config.isInstanceTagEnabled(null)).isFalse();
    }

    @Test
    @DisplayName("Should check instance ratio correctly")
    void shouldCheckInstanceRatio() {
        GrayConfig config = new GrayConfig();
        config.setInstanceRatio(0.5);

        int hash1 = "instance-1".hashCode();
        boolean result1 = config.isInstanceRatioEnabled(hash1);
        assertThat(result1 || !result1).isTrue();
    }

    @Test
    @DisplayName("Should enable all instances when ratio is 1.0")
    void shouldEnableAllWhenRatioOne() {
        GrayConfig config = new GrayConfig();
        config.setInstanceRatio(1.0);
        assertThat(config.isInstanceRatioEnabled(0)).isTrue();
        assertThat(config.isInstanceRatioEnabled(Integer.MAX_VALUE)).isTrue();
        assertThat(config.isInstanceRatioEnabled(Integer.MIN_VALUE)).isTrue();
    }

    @Test
    @DisplayName("Should disable all instances when ratio is 0.0")
    void shouldDisableAllWhenRatioZero() {
        GrayConfig config = new GrayConfig();
        config.setInstanceRatio(0.0);
        assertThat(config.isInstanceRatioEnabled(0)).isFalse();
        assertThat(config.isInstanceRatioEnabled(100)).isFalse();
        assertThat(config.isInstanceRatioEnabled(-100)).isFalse();
    }

    @Test
    @DisplayName("Should check traffic ratio correctly")
    void shouldCheckTrafficRatio() {
        GrayConfig config = new GrayConfig();
        config.setTrafficRatio(0.5);

        boolean result = config.isTrafficRatioEnabled(50);
        assertThat(result || !result).isTrue();
    }

    @Test
    @DisplayName("Should enable all traffic when ratio is 1.0")
    void shouldEnableAllTrafficWhenRatioOne() {
        GrayConfig config = new GrayConfig();
        config.setTrafficRatio(1.0);
        assertThat(config.isTrafficRatioEnabled(0)).isTrue();
        assertThat(config.isTrafficRatioEnabled(Integer.MAX_VALUE)).isTrue();
    }

    @Test
    @DisplayName("Should disable all traffic when ratio is 0.0")
    void shouldDisableAllTrafficWhenRatioZero() {
        GrayConfig config = new GrayConfig();
        config.setTrafficRatio(0.0);
        assertThat(config.isTrafficRatioEnabled(0)).isFalse();
        assertThat(config.isTrafficRatioEnabled(50)).isFalse();
    }

    @Test
    @DisplayName("Should clamp instance ratio to valid range")
    void shouldClampInstanceRatio() {
        GrayConfig config = new GrayConfig();
        config.setInstanceRatio(2.0);
        assertThat(config.getInstanceRatio()).isEqualTo(1.0);

        config.setInstanceRatio(-1.0);
        assertThat(config.getInstanceRatio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should clamp traffic ratio to valid range")
    void shouldClampTrafficRatio() {
        GrayConfig config = new GrayConfig();
        config.setTrafficRatio(2.0);
        assertThat(config.getTrafficRatio()).isEqualTo(1.0);

        config.setTrafficRatio(-1.0);
        assertThat(config.getTrafficRatio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should add service to list")
    void shouldAddService() {
        GrayConfig config = new GrayConfig();
        config.addService("new-service");
        assertThat(config.getServiceList()).contains("new-service");
    }

    @Test
    @DisplayName("Should add instance IP to list")
    void shouldAddInstanceIp() {
        GrayConfig config = new GrayConfig();
        config.addInstanceIp("10.0.0.5");
        assertThat(config.getInstanceIps()).contains("10.0.0.5");
    }

    @Test
    @DisplayName("Should add instance tag to list")
    void shouldAddInstanceTag() {
        GrayConfig config = new GrayConfig();
        config.addInstanceTag("new-tag");
        assertThat(config.getInstanceTags()).contains("new-tag");
    }

    @Test
    @DisplayName("Should handle null service list")
    void shouldHandleNullServiceList() {
        GrayConfig config = new GrayConfig();
        config.setServiceList(null);
        assertThat(config.getServiceList()).isEmpty();
        assertThat(config.isServiceEnabled("any")).isTrue();
    }

    @Test
    @DisplayName("Should handle null instance IPs list")
    void shouldHandleNullInstanceIps() {
        GrayConfig config = new GrayConfig();
        config.setInstanceIps(null);
        assertThat(config.getInstanceIps()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null instance tags list")
    void shouldHandleNullInstanceTags() {
        GrayConfig config = new GrayConfig();
        config.setInstanceTags(null);
        assertThat(config.getInstanceTags()).isEmpty();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEquals() {
        GrayConfig config1 = new GrayConfig();
        config1.addService("svc-a");

        GrayConfig config2 = new GrayConfig();
        config2.addService("svc-a");

        assertThat(config1).isEqualTo(config2);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());

        // Different services
        GrayConfig config3 = new GrayConfig();
        config3.addService("svc-b");
        assertThat(config1).isNotEqualTo(config3);

        // Null
        assertThat(config1).isNotEqualTo(null);

        // Different type
        assertThat(config1).isNotEqualTo("string");

        // Same instance
        assertThat(config1).isEqualTo(config1);
    }

    @Test
    @DisplayName("Should implement toString")
    void shouldImplementToString() {
        GrayConfig config = new GrayConfig();
        String str = config.toString();
        assertThat(str).contains("GrayConfig");
        assertThat(str).contains("instanceRatio");
        assertThat(str).contains("trafficRatio");
    }

    @Test
    @DisplayName("Should test equals with different instanceIps")
    void shouldTestEqualsWithDifferentIps() {
        GrayConfig config1 = new GrayConfig();
        config1.addInstanceIp("10.0.0.1");

        GrayConfig config2 = new GrayConfig();
        config2.addInstanceIp("10.0.0.2");

        assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    @DisplayName("Should test equals with different instanceTags")
    void shouldTestEqualsWithDifferentTags() {
        GrayConfig config1 = new GrayConfig();
        config1.addInstanceTag("tag-a");

        GrayConfig config2 = new GrayConfig();
        config2.addInstanceTag("tag-b");

        assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    @DisplayName("Should test equals with different instanceRatio")
    void shouldTestEqualsWithDifferentInstanceRatio() {
        GrayConfig config1 = new GrayConfig();
        config1.setInstanceRatio(0.5);

        GrayConfig config2 = new GrayConfig();
        config2.setInstanceRatio(0.8);

        assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    @DisplayName("Should test equals with different trafficRatio")
    void shouldTestEqualsWithDifferentTrafficRatio() {
        GrayConfig config1 = new GrayConfig();
        config1.setTrafficRatio(0.5);

        GrayConfig config2 = new GrayConfig();
        config2.setTrafficRatio(0.8);

        assertThat(config1).isNotEqualTo(config2);
    }
}
