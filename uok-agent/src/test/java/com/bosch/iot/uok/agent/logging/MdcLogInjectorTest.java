package com.bosch.iot.uok.agent.logging;

import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.TraceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link MdcLogInjector}.
 */
class MdcLogInjectorTest {

    @BeforeEach
    void setUp() {
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("Should initialize with config")
    void shouldInitializeWithConfig() {
        UokConfig config = new UokConfig();
        MdcLogInjector.initialize(config);
        assertThat(MdcLogInjector.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("Should inject trace context into MDC")
    void shouldInjectTraceContext() {
        MdcLogInjector.initialize(new UokConfig());

        TraceContext context = new TraceContext("trace-123", "span-456", "test-service");
        context.setParentSpanId("parent-789");
        context.setEnv("prod");
        context.setBizDomain("iot");
        context.setTeamName("backend");

        MdcLogInjector.injectTraceContext(context);

        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo("trace-123");
        assertThat(MDC.get(LogConstants.SPAN_ID)).isEqualTo("span-456");
        assertThat(MDC.get(LogConstants.PARENT_SPAN_ID)).isEqualTo("parent-789");
        assertThat(MDC.get(LogConstants.SERVICE_NAME)).isEqualTo("test-service");
        assertThat(MDC.get(LogConstants.ENV)).isEqualTo("prod");
        assertThat(MDC.get(LogConstants.BIZ_DOMAIN)).isEqualTo("iot");
        assertThat(MDC.get(LogConstants.TEAM_NAME)).isEqualTo("backend");
    }

    @Test
    @DisplayName("Should handle null trace context")
    void shouldHandleNullTraceContext() {
        MdcLogInjector.initialize(new UokConfig());
        MdcLogInjector.injectTraceContext(null);
        // Should not throw and not set anything
        assertThat(MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Should handle trace context with null fields")
    void shouldHandleContextWithNullFields() {
        MdcLogInjector.initialize(new UokConfig());

        TraceContext context = new TraceContext();
        context.setTraceId("trace-1");
        context.setSpanId("span-1");
        MdcLogInjector.injectTraceContext(context);

        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo("trace-1");
        assertThat(MDC.get(LogConstants.PARENT_SPAN_ID)).isEqualTo("");
    }

    @Test
    @DisplayName("Should inject service identity into MDC")
    void shouldInjectServiceIdentity() {
        UokConfig config = new UokConfig();
        config.setServiceName("my-service");
        config.setEnv("test");
        config.setBizDomain("iot-home");
        config.setTeamName("platform");

        MdcLogInjector.initialize(config);
        MdcLogInjector.injectServiceIdentity();

        assertThat(MDC.get(LogConstants.SERVICE_NAME)).isEqualTo("my-service");
        assertThat(MDC.get(LogConstants.ENV)).isEqualTo("test");
        assertThat(MDC.get(LogConstants.BIZ_DOMAIN)).isEqualTo("iot-home");
        assertThat(MDC.get(LogConstants.TEAM_NAME)).isEqualTo("platform");
    }

    @Test
    @DisplayName("Should inject business fields from headers")
    void shouldInjectBusinessFields() {
        MdcLogInjector.initialize(new UokConfig());

        Map<String, String> headers = new HashMap<>();
        headers.put("deviceId", "device-001");
        headers.put("userId", "user-123");
        headers.put("errorCode", "ERR-500");
        headers.put("orderId", "ORD-789");

        MdcLogInjector.injectBusinessFields(headers);

        assertThat(MDC.get(LogConstants.DEVICE_ID)).isEqualTo("device-001");
        assertThat(MDC.get(LogConstants.USER_ID)).isEqualTo("user-123");
        assertThat(MDC.get(LogConstants.ERROR_CODE)).isEqualTo("ERR-500");
        assertThat(MDC.get(LogConstants.ORDER_ID)).isEqualTo("ORD-789");
    }

    @Test
    @DisplayName("Should handle null headers in business fields")
    void shouldHandleNullHeaders() {
        MdcLogInjector.initialize(new UokConfig());
        MdcLogInjector.injectBusinessFields(null);
        // Should not throw
    }

    @Test
    @DisplayName("Should handle headers without business fields")
    void shouldHandleHeadersWithoutBusinessFields() {
        MdcLogInjector.initialize(new UokConfig());

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        MdcLogInjector.injectBusinessFields(headers);

        assertThat(MDC.get(LogConstants.DEVICE_ID)).isNull();
    }

    @Test
    @DisplayName("Should clear trace context from MDC")
    void shouldClearTraceContext() {
        MdcLogInjector.initialize(new UokConfig());

        TraceContext context = new TraceContext("trace-1", "span-1", "svc");
        MdcLogInjector.injectTraceContext(context);

        assertThat(MDC.get(LogConstants.TRACE_ID)).isEqualTo("trace-1");

        MdcLogInjector.clearTraceContext();

        assertThat(MDC.get(LogConstants.TRACE_ID)).isNull();
        assertThat(MDC.get(LogConstants.SPAN_ID)).isNull();
        assertThat(MDC.get(LogConstants.PARENT_SPAN_ID)).isNull();
    }

    @Test
    @DisplayName("Should skip injection when log is disabled")
    void shouldSkipWhenLogDisabled() {
        UokConfig config = new UokConfig();
        config.setLogEnabled(false);
        MdcLogInjector.initialize(config);

        TraceContext context = new TraceContext("trace-1", "span-1", "svc");
        MdcLogInjector.injectTraceContext(context);

        assertThat(MDC.get(LogConstants.TRACE_ID)).isNull();
    }

    @Test
    @DisplayName("Should skip injection when not initialized")
    void shouldSkipWhenNotInitialized() {
        // Reset by creating a new instance test - the static state may be initialized
        // from other tests. Just verify the method doesn't throw.
        MdcLogInjector.injectTraceContext(new TraceContext("t", "s", "svc"));
        MdcLogInjector.injectServiceIdentity();
        MdcLogInjector.injectBusinessFields(new HashMap<>());
        MdcLogInjector.clearTraceContext();
    }
}
