package com.bosch.iot.uok.sample.gateway.controller;

import com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation;
import com.bosch.iot.uok.common.context.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Gateway controller that receives requests and forwards to Service A.
 * Demonstrates trace context extraction from incoming request and
 * propagation to downstream services.
 */
@RestController
public class GatewayController {

    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final RestTemplate restTemplate;

    @Value("${service-a.url:http://localhost:8081}")
    private String serviceAUrl;

    public GatewayController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/api/call")
    public Map<String, Object> callDownstream(
            @RequestHeader(value = "traceparent", required = false) String traceParent,
            HttpServletRequest request) {

        // Extract headers for business fields
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        // UOK: Extract or create trace context
        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                traceParent, headers, "sample-gateway");

        log.info("Gateway received request, forwarding to Service A");

        // Propagate trace context to downstream service
        HttpHeaders outgoingHeaders = new HttpHeaders();
        Map<String, String> headerMap = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headerMap);
        headerMap.forEach(outgoingHeaders::set);

        HttpEntity<Void> entity = new HttpEntity<>(outgoingHeaders);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.exchange(
                serviceAUrl + "/api/process", HttpMethod.GET, entity, Map.class)
                .getBody();

        log.info("Gateway received response from Service A");

        // Build response with trace info
        Map<String, Object> result = new HashMap<>();
        if (response != null) {
            result.putAll(response);
        }
        result.put("gatewayTraceId", MDC.get("traceId"));
        result.put("gatewaySpanId", MDC.get("spanId"));

        // Clean up trace context
        HttpServletInstrumentation.onHttpRequestComplete();

        return result;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "sample-gateway");
    }
}
