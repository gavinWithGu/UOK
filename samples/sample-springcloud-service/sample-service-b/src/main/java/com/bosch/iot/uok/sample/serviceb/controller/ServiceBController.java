package com.bosch.iot.uok.sample.serviceb.controller;

import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.common.context.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Service B controller - leaf service in the call chain.
 */
@RestController
public class ServiceBController {

    private static final Logger log = LoggerFactory.getLogger(ServiceBController.class);

    @GetMapping("/api/data")
    public Map<String, Object> getData(
            @RequestHeader(value = "traceparent", required = false) String traceParent,
            HttpServletRequest request) {

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                traceParent, headers, "sample-service-b");

        log.info("Service B returning data");

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Hello from Service B");
        result.put("serviceBTraceId", MDC.get("traceId"));
        result.put("serviceBSpanId", MDC.get("spanId"));

        HttpServletInstrumentation.onHttpRequestComplete();

        return result;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "sample-service-b");
    }
}
