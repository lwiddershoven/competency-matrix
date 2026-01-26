# Rollback Procedure: Quarkus Migration

This document outlines the procedure to rollback the Quarkus 3.30.6 migration and restore the application to the previous Spring Boot state.

## Overview

The migration from Spring Boot to Quarkus 3.30.6 was completed in the `002-platform-modernization` branch. This rollback procedure will revert all changes and restore the Spring Boot implementation.

## Prerequisites

- Git access to the repository
- Database backup (if production rollback)
- Access to the Spring Boot baseline commit

## Rollback Steps

### 1. Identify the Baseline Commit

The last commit before the Quarkus migration began:

```bash
# View recent commits to identify the pre-migration baseline
git log --oneline

# The commit before 002-platform-modernization work started
# This should be tagged or documented
```

### 2. Database Considerations

**Important**: The database schema and data are **unchanged** between Spring Boot and Quarkus versions:
- PostgreSQL 18.1 schema is identical
- Flyway migration V1__initial_schema.sql matches Liquibase baseline
- All data seeding is identical (competencies.yaml unchanged)

**No database rollback is required** - the same database works with both versions.

### 3. Application Rollback Options

#### Option A: Git Revert (Recommended for Production)

Create a revert commit that undoes the migration:

```bash
# Identify the merge commit for 002-platform-modernization
git log --grep="002-platform-modernization" --oneline

# Revert the merge commit
git revert -m 1 <merge-commit-sha>

# Or revert the entire branch worth of commits
git revert <first-migration-commit>..<last-migration-commit>
```

#### Option B: Branch Rollback (Development/Staging)

Switch back to the pre-migration branch:

```bash
# Switch to main (or previous stable branch)
git checkout main

# Or checkout the specific pre-migration commit
git checkout <baseline-commit-sha>
```

#### Option C: Full Reset (Emergency Only)

**Warning**: This discards all migration work

```bash
# Reset to baseline commit (DESTRUCTIVE)
git reset --hard <baseline-commit-sha>

# Force push (if absolutely necessary)
git push --force origin main
```

### 4. Rebuild and Deploy

#### Stop Current Application

```bash
# If running as container
docker-compose down

# If running as jar
# Stop the Quarkus process
```

#### Build Spring Boot Version

```bash
# Clean and build
./mvnw clean package -DskipTests

# Run tests to verify
./mvnw test
```

#### Deploy

```bash
# Start with docker-compose
docker-compose up -d

# Or run the jar
java -jar target/competency-matrix-0.0.1-SNAPSHOT.jar
```

### 5. Verification

Verify the Spring Boot version is running:

```bash
# Check application endpoint
curl http://localhost:8080/

# Check Spring Actuator health
curl http://localhost:9000/actuator/health

# Check metrics
curl http://localhost:9000/actuator/prometheus
```

Expected responses:
- Health check should return Spring Actuator format
- Metrics should show Spring/Micrometer format
- Application logs should show "Spring Boot" branding

### 6. Traffic Management

If performing production rollback:

1. Enable maintenance mode (if available)
2. Drain existing connections
3. Switch load balancer to Spring Boot instances
4. Monitor error rates and latency
5. Disable maintenance mode

## Configuration Differences

Key configuration changes to revert:

### Spring Boot (`application.yaml`)

```yaml
server:
  port: 8080
management:
  server:
    port: 9000
  endpoints:
    web:
      exposure:
        include: health,prometheus
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/competencymatrix
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
```

### Quarkus (`application.properties`)

```properties
quarkus.http.port=8080
quarkus.management.port=9000
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/competencymatrix
quarkus.flyway.migrate-at-start=true
```

## File Differences

### Spring Boot Structure
- Controllers: `src/main/java/.../controller/`
- Thymeleaf templates: `src/main/resources/templates/`
- Liquibase migrations: `src/main/resources/db/changelog/`

### Quarkus Structure
- Resources: `src/main/java/.../resource/`
- Qute templates: `src/main/resources/templates/`
- Flyway migrations: `src/main/resources/db/migration/`

## Monitoring During Rollback

Monitor these metrics during rollback:

1. **Application Health**
   - HTTP 200 responses from health endpoint
   - Application startup time
   - Memory usage

2. **Database Connectivity**
   - Connection pool status
   - Query response times
   - Active connections

3. **User Impact**
   - Page load times
   - Error rates
   - Active user sessions

## Known Issues

1. **URL Compatibility**: All URLs are preserved, no client changes needed
2. **Data Compatibility**: Database schema identical, no data migration needed
3. **Feature Parity**: All features work identically in both versions

## Emergency Contacts

Document relevant contacts for emergency rollback:

- Database Admin: [Contact Info]
- DevOps Team: [Contact Info]
- Application Owner: [Contact Info]

## Post-Rollback Tasks

After successful rollback:

1. Document the rollback reason
2. Create incident report
3. Review monitoring data
4. Update deployment procedures
5. Plan mitigation for issues that caused rollback

## Testing the Rollback Procedure

This rollback procedure should be tested in a non-production environment:

```bash
# In development/staging environment
1. Deploy Quarkus version
2. Verify functionality
3. Execute rollback procedure
4. Verify Spring Boot version
5. Document any issues encountered
```

## Database Schema Reference

For reference, the database schema is managed by:

**Spring Boot**: Liquibase with `db/changelog/db.changelog-master.xml`
**Quarkus**: Flyway with `db/migration/V1__initial_schema.sql`

Both produce identical schemas:
- Tables: role, category, skill, role_skill, career_progression
- All foreign key constraints identical
- All indexes identical
- All data types identical

## Notes

- The migration preserved all functionality, URLs, and behavior
- No database changes means lower rollback risk
- All tests pass in both versions (49 tests)
- Performance characteristics similar (both meet <2s requirement)
