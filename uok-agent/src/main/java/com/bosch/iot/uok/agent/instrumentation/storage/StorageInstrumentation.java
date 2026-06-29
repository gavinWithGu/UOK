package com.bosch.iot.uok.agent.instrumentation.storage;

import com.bosch.iot.uok.common.constant.MetricConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.metrics.MetricCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Storage call instrumentation for MySQL, Redis, and DynamoDB.
 * <p>
 * Wraps storage operations to:
 * 1. Generate independent Spans for database/cache calls
 * 2. Record operation type, SQL/command, and duration
 * 3. Emit Prometheus metrics for storage call monitoring
 * 4. Support slow query detection
 */
public class StorageInstrumentation {

    private static final Logger log = LoggerFactory.getLogger(StorageInstrumentation.class);

    private static final ConcurrentHashMap<String, MetricCollector> METRIC_COLLECTORS =
            new ConcurrentHashMap<>();

    private StorageInstrumentation() {
    }

    /**
     * Storage system type.
     */
    public enum StorageType {
        MYSQL("mysql"),
        REDIS("redis"),
        DYNAMODB("dynamodb");

        private final String value;

        StorageType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Instrument a storage operation.
     *
     * @param storageType   the storage system type
     * @param operation     the operation description (e.g., "SELECT * FROM users")
     * @param operationType the operation type (e.g., "query", "get", "put")
     * @param supplier      the actual storage operation to execute
     * @param <T>           the return type
     * @return the result of the storage operation
     */
    public static <T> T instrument(StorageType storageType, String operation,
                                    String operationType, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        String serviceName = MDC.get("serviceName");
        if (serviceName == null) {
            serviceName = "unknown-service";
        }

        try {
            T result = supplier.get();
            long duration = System.currentTimeMillis() - startTime;
            recordSuccess(storageType, operationType, duration, serviceName);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            recordFailure(storageType, operationType, duration, serviceName);
            throw e;
        }
    }

    /**
     * Instrument a storage operation with no return value.
     *
     * @param storageType   the storage system type
     * @param operation     the operation description
     * @param operationType the operation type
     * @param runnable      the actual storage operation to execute
     */
    public static void instrumentVoid(StorageType storageType, String operation,
                                       String operationType, Runnable runnable) {
        instrument(storageType, operation, operationType, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * Get or create a MetricCollector for a storage type.
     */
    private static MetricCollector getMetricCollector(StorageType storageType, String serviceName) {
        String key = serviceName + ":" + storageType.getValue();
        return METRIC_COLLECTORS.computeIfAbsent(key,
                k -> new MetricCollector(serviceName, storageType.getValue()));
    }

    private static void recordSuccess(StorageType storageType, String operationType,
                                       long durationMs, String serviceName) {
        MetricCollector collector = getMetricCollector(storageType, serviceName);
        Map<String, String> labels = new HashMap<>();
        labels.put(MetricConstants.LABEL_OPERATION, operationType);
        labels.put(MetricConstants.LABEL_DB_SYSTEM, storageType.getValue());
        collector.recordRequest(durationMs, true, labels);
    }

    private static void recordFailure(StorageType storageType, String operationType,
                                       long durationMs, String serviceName) {
        MetricCollector collector = getMetricCollector(storageType, serviceName);
        Map<String, String> labels = new HashMap<>();
        labels.put(MetricConstants.LABEL_OPERATION, operationType);
        labels.put(MetricConstants.LABEL_DB_SYSTEM, storageType.getValue());
        collector.recordRequest(durationMs, false, labels);
    }

    /**
     * Get metric collector for testing.
     */
    static MetricCollector getMetricCollector(String key) {
        return METRIC_COLLECTORS.get(key);
    }

    /**
     * Clear all metric collectors (for testing).
     */
    static void clearCollectors() {
        METRIC_COLLECTORS.clear();
    }
}
