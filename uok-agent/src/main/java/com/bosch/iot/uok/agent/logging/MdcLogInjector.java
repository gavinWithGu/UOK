package com.bosch.iot.uok.agent.logging;

import com.bosch.iot.uok.common.config.UokConfig;
import com.bosch.iot.uok.common.constant.LogConstants;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import org.slf4j.MDC;

import java.util.Map;

/**
 * MDC log injector for automatic trace field injection into logging context.
 * <p>
 * Injects the following fields into SLF4J MDC:
 * - traceId, spanId, parentSpanId (from trace context)
 * - serviceName, env, bizDomain, teamName (from configuration)
 * - Custom business fields (from HTTP headers, Kafka headers, etc.)
 */
public class MdcLogInjector {

    private static volatile UokConfig config;
    private static volatile boolean initialized = false;

    private MdcLogInjector() {
    }

    /**
     * Initialize the log injector with UOK configuration.
     *
     * @param uokConfig the UOK configuration
     */
    public static void initialize(UokConfig uokConfig) {
        config = uokConfig;
        initialized = true;
    }

    /**
     * Inject trace context fields into MDC.
     * Called when a new trace context is established (e.g., at HTTP request entry).
     *
     * @param traceContext the current trace context
     */
    public static void injectTraceContext(TraceContext traceContext) {
        if (!initialized || !config.isLogEnabled()) {
            return;
        }
        if (traceContext == null) {
            return;
        }

        MDC.put(LogConstants.TRACE_ID, nullToEmpty(traceContext.getTraceId()));
        MDC.put(LogConstants.SPAN_ID, nullToEmpty(traceContext.getSpanId()));
        MDC.put(LogConstants.PARENT_SPAN_ID, nullToEmpty(traceContext.getParentSpanId()));
        MDC.put(LogConstants.SERVICE_NAME, nullToEmpty(traceContext.getServiceName()));
        MDC.put(LogConstants.ENV, nullToEmpty(traceContext.getEnv()));
        MDC.put(LogConstants.BIZ_DOMAIN, nullToEmpty(traceContext.getBizDomain()));
        MDC.put(LogConstants.TEAM_NAME, nullToEmpty(traceContext.getTeamName()));
    }

    /**
     * Inject service identity fields into MDC.
     * Called at application startup to set static fields.
     */
    public static void injectServiceIdentity() {
        if (!initialized || config == null) {
            return;
        }
        MDC.put(LogConstants.SERVICE_NAME, config.getServiceName());
        MDC.put(LogConstants.ENV, config.getEnv());
        MDC.put(LogConstants.BIZ_DOMAIN, config.getBizDomain());
        MDC.put(LogConstants.TEAM_NAME, config.getTeamName());
    }

    /**
     * Inject custom business fields from HTTP headers or other sources.
     *
     * @param headers the headers map containing business fields
     */
    public static void injectBusinessFields(Map<String, String> headers) {
        if (!initialized || !config.isLogEnabled() || headers == null) {
            return;
        }

        // Extract common business fields
        String deviceId = headers.get(LogConstants.DEVICE_ID);
        if (deviceId != null) {
            MDC.put(LogConstants.DEVICE_ID, deviceId);
        }

        String userId = headers.get(LogConstants.USER_ID);
        if (userId != null) {
            MDC.put(LogConstants.USER_ID, userId);
        }

        String errorCode = headers.get(LogConstants.ERROR_CODE);
        if (errorCode != null) {
            MDC.put(LogConstants.ERROR_CODE, errorCode);
        }

        String orderId = headers.get(LogConstants.ORDER_ID);
        if (orderId != null) {
            MDC.put(LogConstants.ORDER_ID, orderId);
        }
    }

    /**
     * Clear trace context fields from MDC.
     * Called at the end of a request to prevent context leakage.
     */
    public static void clearTraceContext() {
        MDC.remove(LogConstants.TRACE_ID);
        MDC.remove(LogConstants.SPAN_ID);
        MDC.remove(LogConstants.PARENT_SPAN_ID);
        MDC.remove(LogConstants.SERVICE_NAME);
        MDC.remove(LogConstants.ENV);
        MDC.remove(LogConstants.BIZ_DOMAIN);
        MDC.remove(LogConstants.TEAM_NAME);
        MDC.remove(LogConstants.DEVICE_ID);
        MDC.remove(LogConstants.USER_ID);
        MDC.remove(LogConstants.ERROR_CODE);
        MDC.remove(LogConstants.ORDER_ID);
    }

    /**
     * Check if the log injector is initialized.
     *
     * @return true if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
