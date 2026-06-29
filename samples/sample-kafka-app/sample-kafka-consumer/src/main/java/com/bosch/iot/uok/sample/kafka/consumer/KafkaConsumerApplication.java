package com.bosch.iot.uok.sample.kafka.consumer;

import com.bosch.iot.uok.agent.instrumentation.kafka.KafkaInstrumentation;
import com.bosch.iot.uok.common.context.TraceContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample Kafka Consumer application.
 * Demonstrates trace context extraction from Kafka messages.
 */
@SpringBootApplication
public class KafkaConsumerApplication {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(KafkaConsumerApplication.class, args);
    }

    @org.springframework.stereotype.Component
    public static class MessageConsumer {

        @KafkaListener(topics = "${kafka.topic:test-topic}", groupId = "sample-consumer-group")
        public void consume(ConsumerRecord<String, String> record) {
            // Extract trace context from Kafka headers
            Map<String, byte[]> headers = new HashMap<>();
            record.headers().forEach(header ->
                    headers.put(header.key(), header.value()));

            TraceContext context = KafkaInstrumentation.onConsume(
                    headers, "sample-kafka-consumer");

            log.info("Received message: key={}, value={}, traceId={}",
                    record.key(), record.value(),
                    MDC.get("traceId"));

            // Process the message
            // ... business logic ...

            // Clean up
            KafkaInstrumentation.onConsumeComplete();
        }
    }

    @RestController
    public static class HealthController {

        @GetMapping("/api/health")
        public Map<String, String> health() {
            return Map.of("status", "UP", "service", "sample-kafka-consumer");
        }
    }
}
