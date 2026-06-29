package com.bosch.iot.uok.sample.kafka.producer;

import com.bosch.iot.uok.agent.instrumentation.kafka.KafkaInstrumentation;
import com.bosch.iot.uok.common.context.ContextHolder;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.common.utils.TraceIdGenerator;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample Kafka Producer application.
 * Demonstrates trace context propagation to Kafka messages.
 */
@SpringBootApplication
public class KafkaProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProducerApplication.class, args);
    }

    @RestController
    public static class ProducerController {

        private static final Logger log = LoggerFactory.getLogger(ProducerController.class);

        private final KafkaTemplate<String, String> kafkaTemplate;

        @Value("${kafka.topic:test-topic}")
        private String topic;

        public ProducerController(KafkaTemplate<String, String> kafkaTemplate) {
            this.kafkaTemplate = kafkaTemplate;
        }

        @GetMapping("/api/send")
        public Map<String, String> sendMessage() {
            // Create trace context for this request
            TraceContext context = new TraceContext();
            context.setTraceId(TraceIdGenerator.generateTraceId());
            context.setSpanId(TraceIdGenerator.generateSpanId());
            context.setServiceName("sample-kafka-producer");
            context.setSampled(true);
            ContextHolder.set(context);

            log.info("Sending message to Kafka topic: {}", topic);

            // Create producer record
            String message = "Hello from Kafka Producer at " + System.currentTimeMillis();
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, "key-1", message);

            // Inject trace context into Kafka headers
            Map<String, byte[]> traceHeaders = new HashMap<>();
            KafkaInstrumentation.onProduce(traceHeaders);
            traceHeaders.forEach((k, v) -> record.headers().add(k, v));

            kafkaTemplate.send(record);

            log.info("Message sent successfully");

            Map<String, String> result = new HashMap<>();
            result.put("traceId", context.getTraceId());
            result.put("spanId", context.getSpanId());
            result.put("topic", topic);
            result.put("status", "sent");

            ContextHolder.remove();
            return result;
        }
    }
}
