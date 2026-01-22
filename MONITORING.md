# Post-Deployment Monitoring Plan: Quarkus Migration

This document outlines the monitoring strategy for the Quarkus 3.30.6 migration to ensure successful deployment and early detection of any issues.

## Overview

The migration from Spring Boot to Quarkus 3.30.6 maintains functional equivalence while changing the underlying framework. This monitoring plan focuses on validating equivalent behavior and detecting any regressions.

## Monitoring Timeline

### Phase 1: Immediate (0-2 hours post-deployment)

**Frequency**: Every 5 minutes

**Focus**: Critical functionality and immediate failures

### Phase 2: Short-term (2-24 hours)

**Frequency**: Every 15 minutes

**Focus**: Performance patterns and edge cases

### Phase 3: Medium-term (1-7 days)

**Frequency**: Every hour

**Focus**: Trend analysis and optimization opportunities

### Phase 4: Long-term (7-30 days)

**Frequency**: Daily

**Focus**: Baseline establishment and comparison

## Metrics to Monitor

### 1. Application Health

**Endpoints**:
- `http://localhost:9000/health` - Overall health
- `http://localhost:9000/health/live` - Liveness probe
- `http://localhost:9000/health/ready` - Readiness probe

**Success Criteria**:
```json
{
  "status": "UP",
  "checks": [
    {"name": "Database connections health check", "status": "UP"}
  ]
}
```

**Alerts**:
- Health status != "UP" → P1 alert
- Database health check fails → P1 alert
- Health endpoint responds > 1s → P2 alert

### 2. Performance Metrics

#### Page Load Times

**Baseline (Spring Boot)**:
- Home page: ~100ms
- Role detail: ~150ms
- Compare page: ~200ms
- Fragments: ~50ms

**Target (Quarkus)**:
- Home page: <2s (requirement: SC-003)
- Role detail: <2s
- Compare page: <2s
- Fragments: <1s

**Actual Performance** (from validation tests):
- Home page: 7-10ms ✓
- Role detail: 10-11ms ✓
- Compare page: 8-11ms ✓
- Fragments: 13-145ms ✓

**Monitoring**:
```bash
# HTTP response time monitoring
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/
curl -w "@curl-format.txt" -o /dev/null -s http://localhost:8080/roles/1
```

Where `curl-format.txt` contains:
```
time_namelookup:  %{time_namelookup}\n
time_connect:  %{time_connect}\n
time_appconnect:  %{time_appconnect}\n
time_pretransfer:  %{time_pretransfer}\n
time_redirect:  %{time_redirect}\n
time_starttransfer:  %{time_starttransfer}\n
time_total:  %{time_total}\n
```

**Alerts**:
- Page load > 2s → P2 alert
- Page load > 5s → P1 alert
- 95th percentile > 1s → P3 alert

### 3. Prometheus Metrics

**Endpoint**: `http://localhost:9000/metrics`

**Key Metrics**:

```promql
# HTTP request rate
rate(http_server_requests_seconds_count[5m])

# HTTP request duration (95th percentile)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# HTTP error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# JVM memory usage
jvm_memory_used_bytes / jvm_memory_max_bytes

# Database connection pool
hikaricp_connections_active
hikaricp_connections_idle
```

**Baseline Values** (from Phase 5 validation):
- Total metrics exposed: 56
- Total data points: 167
- JVM metrics: Present ✓
- HTTP server metrics: Present ✓

**Alerts**:
- Error rate > 1% → P2 alert
- Error rate > 5% → P1 alert
- Memory usage > 85% → P2 alert
- Memory usage > 95% → P1 alert
- No active DB connections → P1 alert

### 4. Database Monitoring

**Metrics**:
- Connection pool utilization
- Query execution times
- Active connections
- Failed connections

**Queries to Monitor**:
```sql
-- Check connection count
SELECT count(*) FROM pg_stat_activity WHERE datname = 'competencymatrix';

-- Check long-running queries
SELECT pid, now() - pg_stat_activity.query_start AS duration, query
FROM pg_stat_activity
WHERE state = 'active' AND now() - pg_stat_activity.query_start > interval '1 second';
```

**Alerts**:
- Connection pool exhausted → P1 alert
- Query > 5s → P2 alert
- Failed connections > 5/min → P1 alert

### 5. Application Logs

**Log Locations**:
- Application: stdout/stderr
- Quarkus: `quarkus.log` (if configured)
- Container: Docker logs

**Key Patterns to Monitor**:

```bash
# Error patterns
grep -i "error" /var/log/app.log
grep -i "exception" /var/log/app.log
grep -i "failed" /var/log/app.log

# Startup verification
grep "Quarkus.*started in" /var/log/app.log
grep "Listening on: http" /var/log/app.log
grep "Database seeding complete" /var/log/app.log
```

**Success Indicators**:
```
INFO  [io.quarkus] competency-matrix X.X.X on JVM (powered by Quarkus 3.30.6) started in Xs
INFO  [nl.leonw.competencymatrix.config.DataSeeder] Database seeding complete
```

**Error Indicators**:
- Stack traces
- "Failed to start"
- "Connection refused"
- SQL exceptions

**Alerts**:
- ERROR log level → P2 alert
- Startup failure → P1 alert
- Database connection errors → P1 alert

### 6. Functional Validation

**User Workflows** (manual or automated):

1. **Browse Roles**
   - Access homepage
   - Verify all roles displayed
   - Check role cards render

2. **View Role Details**
   - Click on a role
   - Verify competencies displayed
   - Check category grouping
   - Test skill popups

3. **Compare Roles**
   - Select two roles
   - Compare skills
   - Verify differences highlighted
   - Check navigation

4. **Theme Toggle**
   - Toggle dark/light mode
   - Verify persistence
   - Check visual consistency

**Automated Test**:
```bash
# Run E2E test suite
./mvnw test -Dtest='BrowseCompetenciesTest'
```

**Expected**: 8/8 tests pass

**Alerts**:
- Any E2E test fails → P1 alert
- User-reported functionality issue → P1 alert

## Comparison Dashboard

Create a monitoring dashboard comparing key metrics:

| Metric | Spring Boot Baseline | Quarkus Target | Actual | Status |
|--------|---------------------|----------------|--------|--------|
| Startup Time | ~3s | <5s | 3.0s | ✓ |
| Memory (Initial) | ~200MB | <300MB | TBD | - |
| Home Page Load | ~100ms | <2s | 7-10ms | ✓ |
| Health Check | ~50ms | <200ms | TBD | - |
| Test Pass Rate | 100% (49) | 100% (49) | 100% | ✓ |
| Error Rate | 0% | 0% | TBD | - |

## Alerting Rules

### P1 - Critical (Immediate Response)

- Application health check fails
- Error rate > 5%
- Database connectivity lost
- Memory usage > 95%
- Application crashed/unresponsive

**Response**: Immediate investigation, consider rollback

### P2 - High (15 min response)

- Page load time > 2s
- Error rate > 1%
- Memory usage > 85%
- Database query > 5s
- ERROR logs appearing

**Response**: Investigate within 15 minutes

### P3 - Medium (1 hour response)

- 95th percentile latency > 1s
- Memory usage trending up
- Unexpected WARNING logs

**Response**: Investigate within 1 hour

## Monitoring Tools

### Recommended Setup

1. **Prometheus**
   - Scrape `/metrics` endpoint every 15s
   - Retention: 15 days
   - Storage: Local

2. **Grafana**
   - Dashboard for Quarkus metrics
   - Comparison dashboard (Spring Boot vs Quarkus)
   - Alerting rules configured

3. **Log Aggregation**
   - ELK stack or similar
   - Error pattern detection
   - Log correlation

4. **Synthetic Monitoring**
   - Automated page loads every 5 min
   - Compare against baseline
   - Geographic distribution (if applicable)

### Quick Setup (Docker)

```yaml
# docker-compose.monitoring.yml
version: '3.8'
services:
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
```

```yaml
# prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'competency-matrix'
    static_configs:
      - targets: ['host.docker.internal:9000']
    metrics_path: '/metrics'
```

## Smoke Test Checklist

Execute after deployment:

- [ ] Application starts successfully
- [ ] Health endpoint returns UP
- [ ] Homepage loads in <2s
- [ ] Database connectivity verified
- [ ] Metrics endpoint accessible
- [ ] Role detail page loads
- [ ] Compare functionality works
- [ ] Skill popups display
- [ ] Theme toggle works
- [ ] No errors in logs
- [ ] Memory usage normal
- [ ] All 49 tests pass

## Rollback Triggers

Automatic rollback if:
- Health check fails for >5 minutes
- Error rate >10% for >5 minutes
- Critical functionality broken (P1)
- Memory leak detected (>90% sustained)

Manual rollback consideration if:
- Sustained performance degradation
- Multiple P2 alerts
- User complaints about functionality

## Success Criteria

Migration considered successful after:

1. **24 hours** stable operation with:
   - 0 P1 alerts
   - <5 P2 alerts total
   - Error rate <0.1%
   - Performance within targets

2. **7 days** observation showing:
   - No memory leaks
   - Consistent performance
   - No functionality regressions
   - User satisfaction maintained

## Post-Monitoring Actions

After successful monitoring period:

1. Document actual vs. expected metrics
2. Update baseline values
3. Optimize any identified bottlenecks
4. Adjust alert thresholds based on actual behavior
5. Create incident reports for any issues
6. Share lessons learned
7. Update runbooks

## Contacts

- **On-call Engineer**: [Contact]
- **Database Admin**: [Contact]
- **Product Owner**: [Contact]
- **Escalation**: [Contact]

## References

- Quarkus Metrics Guide: https://quarkus.io/guides/micrometer
- SmallRye Health: https://quarkus.io/guides/smallrye-health
- Migration Spec: `specs/002-platform-modernization/spec.md`
- Rollback Procedure: `ROLLBACK.md`
