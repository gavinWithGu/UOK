package com.bosch.iot.uok.lambda;

import com.bosch.iot.uok.common.config.ConfigLoader;
import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;
import com.bosch.iot.uok.lambda.adapter.KinesisEventAdapter;
import com.bosch.iot.uok.lambda.adapter.KafkaEventAdapter;
import com.bosch.iot.uok.lambda.context.LambdaContextHolder;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.slf4j.MDC;

/**
 * Lambda Tracing Initializer.
 * Automatically initializes OpenTelemetry SDK for AWS Lambda functions.
 * No AWS OTel Layer dependency - pure OpenTelemetry SDK implementation.
 * <p>
 * Usage: Add uok-lambda-starter as Maven dependency.
 * In your Lambda handler, call:
 * <pre>
 *   LambdaTracingInitializer.initialize();
 *   TraceContext context = LambdaTracingInitializer.onLambdaEvent(event);
 *   // ... your handler logic ...
 *   LambdaTracingInitializer.onLambdaComplete();
 * </pre>
 */
public class LambdaTracingInitializer {

    private static final String INSTRUMENTATION_NAME = "com.bosch.iot.uok.lambda";
    private static final String INSTRUMENTATION_VERSION = "1.0.0";

    private static volatile boolean initialized = false;
    private static volatile UokConfig config;
    private static volatile OpenTelemetry openTelemetry;
    private static volatile Tracer tracer;

    private LambdaTracingInitializer() {
    }

    /**
     * Initialize tracing for Lambda function.
     * Thread-safe, will only initialize once.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        // 1. Load configuration
        config = ConfigLoader.load();

        // 2. Initialize OTel SDK
        if (config.isTraceEnabled()) {
            Resource resource = Resource.create(Attributes.builder()
                    .put("service.name", config.getServiceName())
                    .put("deployment.environment", config.getEnv())
                    .build());

            SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                    .setResource(resource)
                    .addSpanProcessor(BatchSpanProcessor.builder(LoggingSpanExporter.create()).build())
                    .setSampler(Sampler.traceIdRatioBased(config.getSamplerConfig().getHeadRate()))
                    .build();

            OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                    .setTracerProvider(tracerProvider)
                    .setPropagators(ContextPropagators.create(
                            W3CTraceContextPropagator.getInstance()))
                    .build();

            openTelemetry = sdk;
            tracer = sdk.getTracer(INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);

            try {
                io.opentelemetry.api.GlobalOpenTelemetry.set(openTelemetry);
            } catch (IllegalStateException e) {
                // Already set, ignore
            }
        }

        initialized = true;
    }

    /**
     * Handle Lambda event - extract or create trace context.
     * Supports Kinesis and Kafka (MSK) events.
     *
     * @param event the Lambda event object
     * @return the established TraceContext
     */
    public static TraceContext onLambdaEvent(Object event) {
        if (!initialized) {
            initialize();
        }

        TraceContext context = null;

        // Try Kinesis event adapter
        if (KinesisEventAdapter.isKinesisEvent(event)) {
            context = KinesisEventAdapter.extractTraceContext(event, config.getServiceName());
        }

        // Try Kafka event adapter
        if (context == null && KafkaEventAdapter.isKafkaEvent(event)) {
            context = KafkaEventAdapter.extractTraceContext(event, config.getServiceName());
        }

        // If no context extracted, create root context
        if (context == null) {
            context = new TraceContext();
            context.setTraceId(TraceIdGenerator.generateTraceId());
            context.setSpanId(TraceIdGenerator.generateSpanId());
            context.setServiceName(config.getServiceName());
            context.setEnv(config.getEnv());
            context.setBizDomain(config.getBizDomain());
            context.setTeamName(config.getTeamName());
            context.setSampled(true);
        }

        // Set in Lambda context holder and MDC
        LambdaContextHolder.set(context);
        MDC.put("traceId", context.getTraceId() != null ? context.getTraceId() : "");
        MDC.put("spanId", context.getSpanId() != null ? context.getSpanId() : "");
        MDC.put("serviceName", config.getServiceName());
        MDC.put("env", config.getEnv());

        return context;
    }

    /**
     * Clean up after Lambda execution.
     */
    public static void onLambdaComplete() {
        MDC.remove("traceId");
        MDC.remove("spanId");
        MDC.remove("parentSpanId");
        MDC.remove("serviceName");
        MDC.remove("env");
        MDC.remove("bizDomain");
        MDC.remove("teamName");
        LambdaContextHolder.clear();
    }

    /**
     * Check if tracing has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the current configuration.
     *
     * @return the UOK configuration
     */
    public static UokConfig getConfig() {
        return config;
    }

    /**
     * Get the OTel tracer.
     *
     * @return the tracer, or null if not initialized
     */
    public static Tracer getTracer() {
        return tracer;
    }

    /**
     * Get the OpenTelemetry instance.
     *
     * @return the OpenTelemetry instance, or null if not initialized
     */
    public static OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }
}
