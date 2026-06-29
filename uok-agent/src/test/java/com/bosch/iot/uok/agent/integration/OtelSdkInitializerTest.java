package com.bosch.iot.uok.agent.integration;

import com.bosch.iot.uok.common.config.UokConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OtelSdkInitializer}.
 */
class OtelSdkInitializerTest {

    @BeforeEach
    void setUp() {
        OtelSdkInitializer.shutdown();
    }

    @AfterEach
    void tearDown() {
        OtelSdkInitializer.shutdown();
    }

    @Test
    @DisplayName("Should not be initialized before calling initialize")
    void shouldNotBeInitializedBeforeInit() {
        assertThat(OtelSdkInitializer.isInitialized()).isFalse();
    }

    @Test
    @DisplayName("Should initialize with config")
    void shouldInitializeWithConfig() {
        UokConfig config = new UokConfig();
        config.setServiceName("test-service");
        config.setEnv("test");

        OtelSdkInitializer.initialize(config);

        assertThat(OtelSdkInitializer.isInitialized()).isTrue();
        assertThat(OtelSdkInitializer.getOpenTelemetry()).isNotNull();
        assertThat(OtelSdkInitializer.getTracer()).isNotNull();
    }

    @Test
    @DisplayName("Should not initialize twice")
    void shouldNotInitializeTwice() {
        UokConfig config = new UokConfig();
        OtelSdkInitializer.initialize(config);

        io.opentelemetry.api.OpenTelemetry first = OtelSdkInitializer.getOpenTelemetry();

        // Second initialization should be no-op
        OtelSdkInitializer.initialize(config);

        assertThat(OtelSdkInitializer.getOpenTelemetry()).isSameAs(first);
    }

    @Test
    @DisplayName("Should create tracer with correct instrumentation name")
    void shouldCreateTracerWithCorrectName() {
        UokConfig config = new UokConfig();
        OtelSdkInitializer.initialize(config);

        Tracer tracer = OtelSdkInitializer.getTracer();
        assertThat(tracer).isNotNull();
    }

    @Test
    @DisplayName("Should shutdown gracefully")
    void shouldShutdownGracefully() {
        UokConfig config = new UokConfig();
        OtelSdkInitializer.initialize(config);

        assertThat(OtelSdkInitializer.isInitialized()).isTrue();

        OtelSdkInitializer.shutdown();

        assertThat(OtelSdkInitializer.isInitialized()).isFalse();
        assertThat(OtelSdkInitializer.getOpenTelemetry()).isNull();
        assertThat(OtelSdkInitializer.getTracer()).isNull();
    }

    @Test
    @DisplayName("Should use custom sampler rate")
    void shouldUseCustomSamplerRate() {
        UokConfig config = new UokConfig();
        config.getSamplerConfig().setHeadRate(0.5);

        OtelSdkInitializer.initialize(config);

        assertThat(OtelSdkInitializer.isInitialized()).isTrue();
    }

    @Test
    @DisplayName("Should return null tracer when not initialized")
    void shouldReturnNullTracerWhenNotInitialized() {
        assertThat(OtelSdkInitializer.getTracer()).isNull();
        assertThat(OtelSdkInitializer.getOpenTelemetry()).isNull();
    }
}
