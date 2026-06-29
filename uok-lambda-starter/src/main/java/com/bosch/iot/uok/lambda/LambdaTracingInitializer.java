package com.bosch.iot.uok.lambda;

/**
 * UOK Lambda Tracing Initializer.
 * Automatically initializes OpenTelemetry SDK for AWS Lambda functions.
 * No AWS OTel Layer dependency - pure OpenTelemetry SDK implementation.
 * <p>
 * Usage: Add uok-lambda-starter as Maven dependency, no business code change required.
 */
public class LambdaTracingInitializer {

    private static volatile boolean initialized = false;

    private LambdaTracingInitializer() {
        // Prevent instantiation
    }

    /**
     * Initialize tracing for Lambda function.
     * Thread-safe, will only initialize once.
     */
    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        // Full implementation will be added in Phase 3
        initialized = true;
    }

    /**
     * Check if tracing has been initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }
}
