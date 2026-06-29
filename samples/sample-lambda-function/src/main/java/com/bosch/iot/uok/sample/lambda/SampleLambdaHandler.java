package com.bosch.iot.uok.sample.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.bosch.iot.uok.common.context.TraceContext;
import com.bosch.iot.uok.lambda.LambdaTracingInitializer;
import com.bosch.iot.uok.lambda.adapter.KinesisEventAdapter;
import com.bosch.iot.uok.lambda.context.LambdaContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample Lambda function handler.
 * Demonstrates UOK Lambda Starter integration:
 * - Trace context extraction from Kinesis events
 * - Automatic MDC injection for log correlation
 * - No AWS OTel Layer dependency
 * <p>
 * Deploy with: uok-lambda-starter as Maven dependency
 */
public class SampleLambdaHandler implements RequestHandler<KinesisEvent, Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(SampleLambdaHandler.class);

    @Override
    public Map<String, Object> handleRequest(KinesisEvent event, Context context) {
        // Initialize UOK tracing (idempotent)
        LambdaTracingInitializer.initialize();

        // Process Kinesis records
        if (event != null && event.getRecords() != null) {
            for (KinesisEvent.KinesisEventRecord record : event.getRecords()) {
                // Extract trace context from Kinesis record
                Map<String, String> headers = new HashMap<>();
                // In real implementation, extract from record metadata
                String partitionKey = record.getKinesis().getPartitionKey();
                if (partitionKey != null) {
                    headers.put("partitionKey", partitionKey);
                }

                // Try to extract trace context from headers
                TraceContext traceContext = KinesisEventAdapter.extractFromHeaders(
                        headers, "sample-lambda-function");

                if (traceContext == null) {
                    // No trace context found, create root via LambdaTracingInitializer
                    traceContext = LambdaTracingInitializer.onLambdaEvent(event);
                } else {
                    LambdaContextHolder.set(traceContext);
                    MDC.put("traceId", traceContext.getTraceId());
                    MDC.put("spanId", traceContext.getSpanId());
                }

                log.info("Processing Kinesis record: partitionKey={}, sequenceNumber={}",
                        partitionKey, record.getKinesis().getSequenceNumber());

                // Process the record data
                byte[] data = record.getKinesis().getData().array();
                String payload = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                log.info("Record payload: {}", payload);
            }
        }

        // Build response with trace info
        Map<String, Object> result = new HashMap<>();
        result.put("statusCode", 200);
        result.put("traceId", MDC.get("traceId"));
        result.put("processedRecords", event != null ? event.getRecords().size() : 0);

        // Clean up
        LambdaTracingInitializer.onLambdaComplete();

        return result;
    }
}
