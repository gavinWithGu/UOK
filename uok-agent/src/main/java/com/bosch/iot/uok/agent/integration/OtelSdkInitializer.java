package com.bosch.iot.uok.agent.integration;

import com.bosch.iot.uok.common.config.UokConfig;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * OpenTelemetry SDK initializer.
 * Sets up the OTel SDK with proper configuration from UokConfig.
 * <p>
 * Supports multiple exporters:
 * - Logging exporter (default for dev)
 * - OTLP gRPC exporter (for production)
 * - Prometheus metrics exporter
 */
public class OtelSdkInitializer {

    private static final String INSTRUMENTATION_NAME = "com.bosch.iot.uok";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";

    private static volatile OpenTelemetry openTelemetry;
    private static volatile Tracer tracer;

    private OtelSdkInitializer() {
    }

    /**
     * Initialize the OpenTelemetry SDK.
     *
     * @param config UOK configuration
     */
    public static synchronized void initialize(UokConfig config) {
        if (openTelemetry != null) {
            return;
        }

        // Create resource with service attributes
        Resource resource = Resource.create(Attributes.builder()
                .put("service.name", config.getServiceName())
                .put("service.version", INSTRUMENTATION_VERSION)
                .put("deployment.environment", config.getEnv())
                .put("biz.domain", config.getBizDomain())
                .put("team.name", config.getTeamName())
                .build());

        // Create span exporter based on configuration
        SpanExporter spanExporter = createSpanExporter(config);

        // Create sampler
        Sampler sampler = Sampler.traceIdRatioBased(config.getSamplerConfig().getHeadRate());

        // Build tracer provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setSampler(sampler)
                .build();

        // Build OpenTelemetry SDK
        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

        openTelemetry = sdk;
        tracer = sdk.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);

        // Register as global
        try {
            io.opentelemetry.api.GlobalOpenTelemetry.set(openTelemetry);
        } catch (IllegalStateException e) {
            // Already set by a previous initialization, ignore
        }
    }

    /**
     * Create span exporter based on configuration.
     */
    private static SpanExporter createSpanExporter(UokConfig config) {
        String otlpEndpoint = System.getProperty("uok.otlp.endpoint");

        if (otlpEndpoint != null && !otlpEndpoint.isEmpty()) {
            return OtlpGrpcSpanExporter.builder()
                    .setEndpoint(otlpEndpoint)
                    .build();
        }

        // Default: logging exporter for development
        return LoggingSpanExporter.create();
    }

    /**
     * Get the OpenTelemetry instance.
     *
     * @return the OpenTelemetry instance, or null if not initialized
     */
    public static OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    /**
     * Get the UOK tracer.
     *
     * @return the tracer instance, or null if not initialized
     */
    public static Tracer getTracer() {
        return tracer;
    }

    /**
     * Check if OTel SDK is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return openTelemetry != null;
    }

    /**
     * Shutdown the OTel SDK gracefully.
     */
    public static synchronized void shutdown() {
        if (openTelemetry instanceof OpenTelemetrySdk sdk) {
            sdk.close();
        }
        openTelemetry = null;
        tracer = null;
    }
}
