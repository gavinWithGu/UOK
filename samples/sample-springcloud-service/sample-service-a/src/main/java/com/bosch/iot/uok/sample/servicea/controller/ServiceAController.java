package com.bosch.iot.uok.sample.servicea.controller;

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
 * Service A controller that processes requests and calls Service B.
 */
@RestController
public class ServiceAController {

    private static final Logger log = LoggerFactory.getLogger(ServiceAController.class);

    private final RestTemplate restTemplate;

    @Value("${service-b.url:http://localhost:8082}")
    private String serviceBUrl;

    public ServiceAController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/api/process")
    public Map<String, Object> process(
            @RequestHeader(value = "traceparent", required = false) String traceParent,
            HttpServletRequest request) {

        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        TraceContext context = HttpServletInstrumentation.onHttpRequest(
                traceParent, headers, "sample-service-a");

        log.info("Service A processing request, calling Service B");

        HttpHeaders outgoingHeaders = new HttpHeaders();
        Map<String, String> headerMap = new HashMap<>();
        HttpServletInstrumentation.onHttpResponse(headerMap);
        headerMap.forEach(outgoingHeaders::set);

        HttpEntity<Void> entity = new HttpEntity<>(outgoingHeaders);
        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.exchange(
                serviceBUrl + "/api/data", HttpMethod.GET, entity, Map.class)
                .getBody();

        log.info("Service A received response from Service B");

        Map<String, Object> result = new HashMap<>();
        if (response != null) {
            result.putAll(response);
        }
        result.put("serviceATraceId", MDC.get("traceId"));
        result.put("serviceASpanId", MDC.get("spanId"));

        HttpServletInstrumentation.onHttpRequestComplete();

        return result;
    }

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "sample-service-a");
    }
}
