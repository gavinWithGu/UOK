package com.bosch.iot.uok.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogConstants}.
 * Verifies constant values are correctly defined and accessible.
 */
class LogConstantsTest {

    @Test
    @DisplayName("Should define standard trace fields")
    void shouldDefineStandardTraceFields() {
        assertThat(LogConstants.TRACE_ID).isEqualTo("traceId");
        assertThat(LogConstants.SPAN_ID).isEqualTo("spanId");
        assertThat(LogConstants.PARENT_SPAN_ID).isEqualTo("parentSpanId");
    }

    @Test
    @DisplayName("Should define service identity fields")
    void shouldDefineServiceIdentityFields() {
        assertThat(LogConstants.SERVICE_NAME).isEqualTo("serviceName");
        assertThat(LogConstants.ENV).isEqualTo("env");
        assertThat(LogConstants.BIZ_DOMAIN).isEqualTo("bizDomain");
        assertThat(LogConstants.TEAM_NAME).isEqualTo("teamName");
    }

    @Test
    @DisplayName("Should define standard log fields")
    void shouldDefineStandardLogFields() {
        assertThat(LogConstants.TIMESTAMP).isEqualTo("timestamp");
        assertThat(LogConstants.LEVEL).isEqualTo("level");
        assertThat(LogConstants.LOGGER).isEqualTo("logger");
        assertThat(LogConstants.THREAD).isEqualTo("thread");
        assertThat(LogConstants.MESSAGE).isEqualTo("message");
    }

    @Test
    @DisplayName("Should define business extension fields")
    void shouldDefineBusinessExtensionFields() {
        assertThat(LogConstants.DEVICE_ID).isEqualTo("deviceId");
        assertThat(LogConstants.USER_ID).isEqualTo("userId");
        assertThat(LogConstants.ERROR_CODE).isEqualTo("errorCode");
        assertThat(LogConstants.ORDER_ID).isEqualTo("orderId");
    }

    @Test
    @DisplayName("Should define W3C trace context headers")
    void shouldDefineW3cHeaders() {
        assertThat(LogConstants.TRACE_PARENT_HEADER).isEqualTo("traceparent");
        assertThat(LogConstants.TRACE_STATE_HEADER).isEqualTo("tracestate");
    }

    @Test
    @DisplayName("Should define Kafka header keys")
    void shouldDefineKafkaHeaderKeys() {
        assertThat(LogConstants.KAFKA_TRACE_ID_KEY).isEqualTo("traceId");
        assertThat(LogConstants.KAFKA_SPAN_ID_KEY).isEqualTo("spanId");
        assertThat(LogConstants.KAFKA_PARENT_SPAN_ID_KEY).isEqualTo("parentSpanId");
        assertThat(LogConstants.KAFKA_SAMPLED_KEY).isEqualTo("sampled");
    }
}
