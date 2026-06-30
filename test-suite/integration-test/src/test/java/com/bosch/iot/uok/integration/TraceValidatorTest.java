package com.bosch.iot.uok.integration;

import com.bosch.iot.uok.integration.TraceValidator.LogEntry;
import com.bosch.iot.uok.integration.TraceValidator.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TraceValidator - the automated trace chain validation utility.
 */
class TraceValidatorTest {

    @TempDir
    Path tempDir;

    // =================== In-Memory Validation ===================

    @Test
    @DisplayName("Should validate a correct single-trace chain")
    void shouldValidateCorrectSingleTraceChain() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("abc123", "span1", null, "gateway", "INFO"),
                new LogEntry("abc123", "span2", "span1", "service-a", "INFO"),
                new LogEntry("abc123", "span3", "span2", "service-b", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getTraceCount()).isEqualTo(1);
        assertThat(result.getEntryCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should validate multiple independent traces")
    void shouldValidateMultipleTraces() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("trace1", "s1", null, "svc-a", "INFO"),
                new LogEntry("trace1", "s2", "s1", "svc-b", "INFO"),
                new LogEntry("trace2", "s3", null, "svc-c", "INFO"),
                new LogEntry("trace2", "s4", "s3", "svc-d", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getTraceCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should detect missing traceId")
    void shouldDetectMissingTraceId() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry(null, "span1", null, "gateway", "INFO"),
                new LogEntry("abc123", "span2", "span1", "service-a", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Missing traceId"));
    }

    @Test
    @DisplayName("Should detect missing spanId")
    void shouldDetectMissingSpanId() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("abc123", null, null, "gateway", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Missing spanId"));
    }

    @Test
    @DisplayName("Should detect broken parent-child linkage")
    void shouldDetectBrokenParentChildLinkage() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("abc123", "span1", null, "gateway", "INFO"),
                new LogEntry("abc123", "span2", "nonexistent-parent", "service-a", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("parentSpanId") && e.contains("not found"));
    }

    @Test
    @DisplayName("Should detect missing root entry")
    void shouldDetectMissingRootEntry() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("abc123", "span1", "span0", "service-a", "INFO"),
                new LogEntry("abc123", "span2", "span1", "service-b", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("No root entry"));
    }

    @Test
    @DisplayName("Should detect unsampled error log")
    void shouldDetectUnsampledErrorLog() {
        LogEntry entry = new LogEntry("abc123", "span1", null, "gateway", "ERROR");
        entry.sampled = false;

        List<LogEntry> entries = Collections.singletonList(entry);
        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("Error log entry is unsampled"));
    }

    @Test
    @DisplayName("Should allow sampled error log")
    void shouldAllowSampledErrorLog() {
        LogEntry entry = new LogEntry("abc123", "span1", null, "gateway", "ERROR");
        entry.sampled = true;

        List<LogEntry> entries = Collections.singletonList(entry);
        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should allow error log without sampled field")
    void shouldAllowErrorLogWithoutSampledField() {
        LogEntry entry = new LogEntry("abc123", "span1", null, "gateway", "ERROR");
        // sampled is null (not set)

        List<LogEntry> entries = Collections.singletonList(entry);
        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should validate specific trace chain")
    void shouldValidateSpecificTraceChain() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("trace1", "s1", null, "svc-a", "INFO"),
                new LogEntry("trace1", "s2", "s1", "svc-b", "INFO"),
                new LogEntry("trace2", "s3", null, "svc-c", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validateTraceChain("trace1");
        assertThat(result.isValid()).isTrue();
        assertThat(result.getEntryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should report error for non-existent traceId")
    void shouldReportErrorForNonExistentTraceId() {
        List<LogEntry> entries = Collections.singletonList(
                new LogEntry("trace1", "s1", null, "svc-a", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validateTraceChain("nonexistent");
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("No entries found"));
    }

    @Test
    @DisplayName("Should build trace topology")
    void shouldBuildTraceTopology() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("trace1", "s1", null, "gateway", "INFO"),
                new LogEntry("trace1", "s2", "s1", "service-a", "INFO"),
                new LogEntry("trace1", "s3", "s2", "service-b", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.getTopology()).containsKey("trace1");
        List<String> chain = result.getTopology().get("trace1");
        assertThat(chain).hasSize(3);
        assertThat(chain.get(0)).contains("gateway");
    }

    @Test
    @DisplayName("Should validate root entry with empty parentSpanId")
    void shouldValidateRootWithEmptyParentSpanId() {
        LogEntry entry = new LogEntry("abc123", "span1", "", "gateway", "INFO");

        Result result = TraceValidator.fromEntries(Collections.singletonList(entry)).validate();
        assertThat(result.isValid()).isTrue();
    }

    // =================== File-Based Validation ===================

    @Test
    @DisplayName("Should parse JSON log file and validate")
    void shouldParseJsonLogFileAndValidate() throws Exception {
        Path logFile = tempDir.resolve("test-app.log");
        String json1 = "{\"traceId\":\"abc123\",\"spanId\":\"span1\",\"parentSpanId\":null,\"serviceName\":\"gateway\",\"level\":\"INFO\"}";
        String json2 = "{\"traceId\":\"abc123\",\"spanId\":\"span2\",\"parentSpanId\":\"span1\",\"serviceName\":\"service-a\",\"level\":\"INFO\"}";
        java.nio.file.Files.writeString(logFile, json1 + "\n" + json2 + "\n");

        Result result = TraceValidator.fromLogFile(logFile).validate();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getEntryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should skip non-JSON lines in log file")
    void shouldSkipNonJsonLines() throws Exception {
        Path logFile = tempDir.resolve("mixed.log");
        java.nio.file.Files.writeString(logFile,
                "some plain text\n" +
                        "{\"traceId\":\"abc\",\"spanId\":\"s1\",\"parentSpanId\":null,\"serviceName\":\"svc\",\"level\":\"INFO\"}\n" +
                        "\n" +
                        "another plain line\n");

        Result result = TraceValidator.fromLogFile(logFile).validate();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getEntryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should report error for empty log file")
    void shouldReportErrorForEmptyLogFile() throws Exception {
        Path logFile = tempDir.resolve("empty.log");
        java.nio.file.Files.writeString(logFile, "");

        Result result = TraceValidator.fromLogFile(logFile).validate();
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("No log entries"));
    }

    @Test
    @DisplayName("Should validate a deep multi-hop chain")
    void shouldValidateDeepMultiHopChain() {
        List<LogEntry> entries = Arrays.asList(
                new LogEntry("trace1", "s0", null, "gateway", "INFO"),
                new LogEntry("trace1", "s1", "s0", "svc-a", "INFO"),
                new LogEntry("trace1", "s2", "s1", "svc-b", "INFO"),
                new LogEntry("trace1", "s3", "s2", "svc-c", "INFO"),
                new LogEntry("trace1", "s4", "s3", "svc-d", "INFO")
        );

        Result result = TraceValidator.fromEntries(entries).validate();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getEntryCount()).isEqualTo(5);
    }
}
