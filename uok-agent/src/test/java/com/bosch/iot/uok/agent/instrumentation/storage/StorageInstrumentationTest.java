package com.bosch.iot.uok.agent.instrumentation.storage;

import com.bosch.iot.uok.agent.logging.MdcLogInjector;
import com.bosch.iot.uok.common.config.UokConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StorageInstrumentation}.
 */
class StorageInstrumentationTest {

    @BeforeEach
    void setUp() {
        StorageInstrumentation.clearCollectors();
        MdcLogInjector.initialize(new UokConfig());
    }

    @Test
    @DisplayName("Should instrument MySQL query successfully")
    void shouldInstrumentMysqlQuery() {
        MDC.put("serviceName", "test-service");

        String result = StorageInstrumentation.instrument(
                StorageInstrumentation.StorageType.MYSQL,
                "SELECT * FROM users WHERE id = ?",
                "query",
                () -> "query-result");

        assertThat(result).isEqualTo("query-result");
    }

    @Test
    @DisplayName("Should instrument Redis get operation")
    void shouldInstrumentRedisGet() {
        MDC.put("serviceName", "test-service");

        String result = StorageInstrumentation.instrument(
                StorageInstrumentation.StorageType.REDIS,
                "GET user:123",
                "get",
                () -> "cached-value");

        assertThat(result).isEqualTo("cached-value");
    }

    @Test
    @DisplayName("Should instrument DynamoDB put operation")
    void shouldInstrumentDynamoPut() {
        MDC.put("serviceName", "test-service");

        StorageInstrumentation.instrumentVoid(
                StorageInstrumentation.StorageType.DYNAMODB,
                "PutItem users",
                "put",
                () -> {});
        // Should not throw
    }

    @Test
    @DisplayName("Should record failure on exception")
    void shouldRecordFailureOnException() {
        MDC.put("serviceName", "test-service");

        assertThatThrownBy(() ->
                StorageInstrumentation.instrument(
                        StorageInstrumentation.StorageType.MYSQL,
                        "BAD QUERY",
                        "query",
                        () -> { throw new RuntimeException("DB error"); }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB error");
    }

    @Test
    @DisplayName("Should work without MDC service name")
    void shouldWorkWithoutMdcServiceName() {
        MDC.remove("serviceName");

        String result = StorageInstrumentation.instrument(
                StorageInstrumentation.StorageType.REDIS,
                "PING",
                "ping",
                () -> "PONG");

        assertThat(result).isEqualTo("PONG");
    }

    @Test
    @DisplayName("StorageType enum should have correct values")
    void storageTypeShouldHaveCorrectValues() {
        assertThat(StorageInstrumentation.StorageType.MYSQL.getValue()).isEqualTo("mysql");
        assertThat(StorageInstrumentation.StorageType.REDIS.getValue()).isEqualTo("redis");
        assertThat(StorageInstrumentation.StorageType.DYNAMODB.getValue()).isEqualTo("dynamodb");
    }

    @Test
    @DisplayName("Should handle multiple operations")
    void shouldHandleMultipleOperations() {
        MDC.put("serviceName", "multi-service");

        // Multiple operations should not interfere
        StorageInstrumentation.instrument(StorageInstrumentation.StorageType.MYSQL,
                "query1", "select", () -> "r1");
        StorageInstrumentation.instrument(StorageInstrumentation.StorageType.REDIS,
                "get1", "get", () -> "r2");
        StorageInstrumentation.instrument(StorageInstrumentation.StorageType.DYNAMODB,
                "put1", "put", () -> "r3");
    }
}
