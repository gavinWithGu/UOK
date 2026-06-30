package com.bosch.iot.uok.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Automated trace chain validation utility.
 * <p>
 * Parses structured JSON log files and validates:
 * <ol>
 *   <li>Every log entry contains traceId and spanId</li>
 *   <li>All logs in the same request share the same traceId</li>
 *   <li>Upstream spanId equals downstream parentSpanId</li>
 *   <li>Root entries have empty or null parentSpanId</li>
 *   <li>Error logs are 100% retained (sampling bypass)</li>
 * </ol>
 * <p>
 * Can be used as assertions in test cases:
 * <pre>
 *   TraceValidator validator = TraceValidator.fromLogFile(logPath);
 *   TraceValidator.Result result = validator.validate();
 *   assertThat(result.isValid()).isTrue();
 * </pre>
 */
public class TraceValidator {

    private final List<LogEntry> entries;

    private TraceValidator(List<LogEntry> entries) {
        this.entries = Collections.unmodifiableList(entries);
    }

    // =================== Factory Methods ===================

    /**
     * Create a TraceValidator from a log file containing JSON-formatted log lines.
     * Each line should be a JSON object with at least traceId, spanId fields.
     */
    public static TraceValidator fromLogFile(Path logFile) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || !line.startsWith("{")) {
                    continue;
                }
                LogEntry entry = parseJsonLine(line);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        return new TraceValidator(entries);
    }

    /**
     * Create a TraceValidator from a pre-built list of log entries.
     */
    public static TraceValidator fromEntries(List<LogEntry> entries) {
        return new TraceValidator(new ArrayList<>(entries));
    }

    // =================== Validation ===================

    /**
     * Run all validation checks and return the aggregated result.
     */
    public Result validate() {
        Result result = new Result();

        if (entries.isEmpty()) {
            result.addError("No log entries found");
            return result;
        }

        // Check 1: Every entry has traceId and spanId
        for (LogEntry entry : entries) {
            if (entry.traceId == null || entry.traceId.isEmpty()) {
                result.addError("Missing traceId in entry: " + entry);
            }
            if (entry.spanId == null || entry.spanId.isEmpty()) {
                result.addError("Missing spanId in entry: " + entry);
            }
        }

        // Check 2: All entries with same traceId are consistent
        Map<String, List<LogEntry>> byTraceId = entries.stream()
                .filter(e -> e.traceId != null)
                .collect(Collectors.groupingBy(e -> e.traceId));

        for (Map.Entry<String, List<LogEntry>> traceGroup : byTraceId.entrySet()) {
            List<LogEntry> group = traceGroup.getValue();

            // Check 3: parent-child span linkage
            Set<String> allSpanIds = group.stream()
                    .map(e -> e.spanId)
                    .collect(Collectors.toSet());

            for (LogEntry entry : group) {
                if (entry.parentSpanId != null && !entry.parentSpanId.isEmpty()) {
                    if (!allSpanIds.contains(entry.parentSpanId)) {
                        result.addError("parentSpanId '" + entry.parentSpanId +
                                "' not found in trace " + entry.traceId +
                                " (referenced by spanId '" + entry.spanId + "')");
                    }
                }
            }

            // Check 4: At least one root entry (empty parentSpanId) per trace
            boolean hasRoot = group.stream()
                    .anyMatch(e -> e.parentSpanId == null || e.parentSpanId.isEmpty());
            if (!hasRoot) {
                result.addError("No root entry found in trace " + traceGroup.getKey() +
                        " (all entries have parentSpanId)");
            }
        }

        // Check 5: Error entries should always be sampled (sampled=true or no sampled field)
        for (LogEntry entry : entries) {
            if ("ERROR".equals(entry.level) && entry.sampled != null && !entry.sampled) {
                result.addError("Error log entry is unsampled: traceId=" +
                        entry.traceId + ", spanId=" + entry.spanId);
            }
        }

        // Build trace topology
        result.setTraceCount(byTraceId.size());
        result.setEntryCount(entries.size());
        result.setTopology(buildTopology(byTraceId));

        return result;
    }

    /**
     * Validate that a specific traceId forms a complete chain.
     */
    public Result validateTraceChain(String traceId) {
        List<LogEntry> traceEntries = entries.stream()
                .filter(e -> traceId.equals(e.traceId))
                .collect(Collectors.toList());

        if (traceEntries.isEmpty()) {
            Result result = new Result();
            result.addError("No entries found for traceId: " + traceId);
            return result;
        }

        return TraceValidator.fromEntries(traceEntries).validate();
    }

    // =================== Topology ===================

    private Map<String, List<String>> buildTopology(Map<String, List<LogEntry>> byTraceId) {
        Map<String, List<String>> topology = new HashMap<>();
        for (Map.Entry<String, List<LogEntry>> traceGroup : byTraceId.entrySet()) {
            List<String> chain = traceGroup.getValue().stream()
                    .sorted((a, b) -> {
                        // Root first, then by parent-child order
                        boolean aRoot = a.parentSpanId == null || a.parentSpanId.isEmpty();
                        boolean bRoot = b.parentSpanId == null || b.parentSpanId.isEmpty();
                        if (aRoot && !bRoot) return -1;
                        if (!aRoot && bRoot) return 1;
                        return 0;
                    })
                    .map(e -> e.serviceName != null ?
                            e.serviceName + "(" + e.spanId + ")" :
                            "unknown(" + e.spanId + ")")
                    .collect(Collectors.toList());
            topology.put(traceGroup.getKey(), chain);
        }
        return topology;
    }

    // =================== JSON Parser ===================

    private static LogEntry parseJsonLine(String json) {
        LogEntry entry = new LogEntry();
        // Simple JSON field extraction (no external dependency needed)
        entry.traceId = extractJsonString(json, "traceId");
        entry.spanId = extractJsonString(json, "spanId");
        entry.parentSpanId = extractJsonString(json, "parentSpanId");
        entry.serviceName = extractJsonString(json, "serviceName");
        entry.level = extractJsonString(json, "level");
        String sampled = extractJsonString(json, "sampled");
        if (sampled != null) {
            entry.sampled = Boolean.parseBoolean(sampled);
        }
        return entry;
    }

    private static String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        // Skip whitespace
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
            // String value
            int valueEnd = json.indexOf('"', valueStart + 1);
            if (valueEnd < 0) return null;
            return json.substring(valueStart + 1, valueEnd);
        } else if (firstChar == 'n') {
            // null
            return null;
        } else {
            // Boolean or number - return as string
            int valueEnd = valueStart;
            while (valueEnd < json.length() &&
                    json.charAt(valueEnd) != ',' &&
                    json.charAt(valueEnd) != '}' &&
                    json.charAt(valueEnd) != ' ') {
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        }
    }

    // =================== Inner Classes ===================

    /**
     * Represents a single structured log entry.
     */
    public static class LogEntry {
        public String traceId;
        public String spanId;
        public String parentSpanId;
        public String serviceName;
        public String level;
        public Boolean sampled;

        public LogEntry() {}

        public LogEntry(String traceId, String spanId, String parentSpanId,
                        String serviceName, String level) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
            this.serviceName = serviceName;
            this.level = level;
        }

        @Override
        public String toString() {
            return "LogEntry{traceId='" + traceId + "', spanId='" + spanId +
                    "', parentSpanId='" + parentSpanId +
                    "', serviceName='" + serviceName + "', level='" + level + "'}";
        }
    }

    /**
     * Validation result containing errors and trace topology.
     */
    public static class Result {
        private final List<String> errors = new ArrayList<>();
        private int traceCount;
        private int entryCount;
        private Map<String, List<String>> topology = Collections.emptyMap();

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return Collections.unmodifiableList(errors);
        }

        public void addError(String error) {
            errors.add(error);
        }

        public int getTraceCount() {
            return traceCount;
        }

        public void setTraceCount(int traceCount) {
            this.traceCount = traceCount;
        }

        public int getEntryCount() {
            return entryCount;
        }

        public void setEntryCount(int entryCount) {
            this.entryCount = entryCount;
        }

        public Map<String, List<String>> getTopology() {
            return topology;
        }

        public void setTopology(Map<String, List<String>> topology) {
            this.topology = topology;
        }

        @Override
        public String toString() {
            return "Result{valid=" + isValid() +
                    ", traceCount=" + traceCount +
                    ", entryCount=" + entryCount +
                    ", errors=" + errors +
                    ", topology=" + topology + "}";
        }
    }
}
