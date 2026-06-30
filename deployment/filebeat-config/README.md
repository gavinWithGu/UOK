# Filebeat Configuration for UOK

This directory contains the standard Filebeat configuration templates for shipping UOK trace logs to OpenSearch.

## Files

| File | Description |
|------|-------------|
| `filebeat.yml` | Main Filebeat configuration with JSON log parsing and OpenSearch output |
| `opensearch-index-template.json` | OpenSearch index template with optimized field mappings |

## Quick Start

### 1. Deploy Filebeat Configuration

```bash
# Copy config to Filebeat config directory
sudo cp filebeat.yml /etc/filebeat/filebeat.yml

# Set environment variables
export OPENSEARCH_HOST=your-opensearch-host.example.com
export OPENSEARCH_USERNAME=filebeat_internal
export OPENSEARCH_PASSWORD=your-secure-password
export ENV=production
export AWS_REGION=eu-central-1
```

### 2. Deploy OpenSearch Index Template

```bash
curl -X PUT "https://${OPENSEARCH_HOST}:9200/_index_template/uok-trace" \
  -H "Content-Type: application/json" \
  -u "${OPENSEARCH_USERNAME}:${OPENSEARCH_PASSWORD}" \
  -d @opensearch-index-template.json
```

### 3. Verify Configuration

```bash
# Test Filebeat config
filebeat test config -c /etc/filebeat/filebeat.yml

# Test connection to OpenSearch
filebeat test output -c /etc/filebeat/filebeat.yml
```

### 4. Start Filebeat

```bash
sudo systemctl enable filebeat
sudo systemctl start filebeat
```

## Log Format

UOK outputs JSON-structured logs with the following standard fields:

```json
{
  "timestamp": "2026-06-29T10:00:00.000Z",
  "level": "INFO",
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "abcdef0123456789",
  "parentSpanId": "1234567890abcdef",
  "serviceName": "sample-service-a",
  "bizDomain": "iot-home",
  "teamName": "backend-team",
  "env": "test",
  "thread": "http-nio-8080-exec-1",
  "logger": "com.bosch.iot.uok.agent.instrumentation.http.HttpServletInstrumentation",
  "message": "HTTP request traced"
}
```

## Index Routing

Logs are routed to OpenSearch indices by service name and date:

```
uok-trace-{serviceName}-{yyyy.MM.dd}
```

Example: `uok-trace-sample-service-a-2026.06.29`

## Document-Level Security (DLS)

To restrict teams to their own logs, configure DLS in OpenSearch:

```json
{
  "query": {
    "term": { "teamName": "backend-team" }
  }
}
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `OPENSEARCH_HOST` | (required) | OpenSearch host |
| `OPENSEARCH_USERNAME` | `filebeat_internal` | OpenSearch username |
| `OPENSEARCH_PASSWORD` | (required) | OpenSearch password |
| `ENV` | `production` | Environment label |
| `AWS_REGION` | `eu-central-1` | AWS region for Lambda logs |
