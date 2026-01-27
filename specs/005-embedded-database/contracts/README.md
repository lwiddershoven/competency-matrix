# API Contracts

**Feature**: 005-embedded-database
**Date**: 2026-01-27

## Overview

This feature does NOT introduce new API endpoints or change existing contracts. The migration from PostgreSQL to H2 in-memory database is **transparent to all API consumers**.

## Existing API Contracts (Unchanged)

The competency matrix application exposes the following endpoints via JAX-RS:

### GET /

**Purpose**: Serves the competency matrix overview page (server-side rendered HTML)

**Request**: None
**Response**: HTML page with competency matrix

**Database Interaction**:
- Fetches all roles via `RoleRepository.findAllOrderByFamilyAndSeniority()`
- Fetches all skills via `SkillRepository.findAllOrderByName()`
- Fetches all requirements via `RoleSkillRequirementRepository.findAll()`

**H2 Migration Impact**: None - query patterns unchanged

---

### Health Checks (Quarkus SmallRye Health)

**Endpoints**:
- `GET /q/health` - Overall health status
- `GET /q/health/live` - Liveness probe
- `GET /q/health/ready` - Readiness probe

**Database Health Check**:
Quarkus automatically monitors datasource health. H2 in-memory database will report as healthy when:
- Connection pool is active
- Database is accessible
- Flyway migrations completed successfully

**H2 Migration Impact**: None - health checks are database-agnostic

---

### Metrics (Quarkus Micrometer)

**Endpoints**:
- `GET /q/metrics` - Prometheus-format metrics

**Database Metrics** (unchanged):
- Connection pool statistics
- Query execution times
- Transaction counts

**H2 Migration Impact**: Metrics will reflect H2 connection pool instead of PostgreSQL pool

---

## Internal Contracts (Repository Layer)

### Repository Interface Pattern

All repositories follow consistent method signatures:

```java
// Find operations
Optional<T> findById(Integer id)
Optional<T> findByName(String name)
List<T> findAll()

// Create/Update operations
T save(T entity)            // Returns entity with generated ID
void update(T entity)

// Delete operations
void deleteById(Integer id)
void deleteAll()
```

**H2 Migration Impact**:
- `save()` method implementation changes (RETURNING → generated keys API)
- Method signatures unchanged
- Return types unchanged
- All existing call sites continue working

### JDBC Contract Changes

#### Before (PostgreSQL-specific):
```java
// Uses RETURNING clause
String sql = "INSERT INTO table (col1) VALUES (?) RETURNING id";
PreparedStatement stmt = conn.prepareStatement(sql);
stmt.setString(1, value);
ResultSet rs = stmt.executeQuery();  // executeQuery for RETURNING
if (rs.next()) {
    return rs.getInt("id");
}
```

#### After (JDBC standard):
```java
// Uses RETURN_GENERATED_KEYS
String sql = "INSERT INTO table (col1) VALUES (?)";
PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
stmt.setString(1, value);
int affected = stmt.executeUpdate();  // executeUpdate for INSERT
ResultSet generatedKeys = stmt.getGeneratedKeys();
if (generatedKeys.next()) {
    return generatedKeys.getInt(1);  // Column index, not name
}
```

**Benefits**:
- More portable (works with PostgreSQL, H2, MySQL, etc.)
- Standard JDBC API (not database-specific extension)
- Better practice for multi-database support

## Data Contracts (YAML Schema)

### competencies.yaml Structure (Unchanged)

```yaml
categories:
  - name: string (required, unique)
    displayOrder: integer (required)
    skills:
      - name: string (required, unique within category)
        basic: string (required)
        decent: string (required)
        good: string (required)
        excellent: string (required)

roles:
  - name: string (required, unique)
    description: string (optional)
    roleFamily: string (required, one of: Developer, Architect, Operations, Other)
    seniorityOrder: integer (required, 1-5)
    requirements:
      - skill: string (required, must reference existing skill name)
        level: string (required, one of: BASIC, DECENT, GOOD, EXCELLENT)

progressions:
  - from: string (required, must reference existing role name)
    to: string (required, must reference existing role name)
```

**Validation Rules** (unchanged):
- All required fields must be present
- Skill references must resolve to defined skills
- Role references must resolve to defined roles
- Enum values must match defined constants
- Names must be unique within their scope

**H2 Migration Impact**: None - YAML parsing and validation are database-independent

## Backwards Compatibility

### Database Provider Abstraction

The application can support multiple database providers via configuration:

```properties
# H2 in-memory (default for development)
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:competencymatrix;MODE=PostgreSQL

# PostgreSQL (optional for production/testing)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/competencymatrix
```

**Contract Guarantee**: Applications using environment variables to configure PostgreSQL will continue working after migration, provided repository code is updated to use generated keys API instead of RETURNING clause.

## Testing Contracts

### Test Database Configuration

**Before**:
```properties
%test.quarkus.datasource.devservices.enabled=true
%test.quarkus.datasource.devservices.image-name=postgres:18.1
```

**After**:
```properties
%test.quarkus.datasource.db-kind=h2
%test.quarkus.datasource.jdbc.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
```

**Test Contract**: All integration tests should pass without modification. Tests interact with repositories and services, not directly with database-specific APIs.

## Migration Contract

### Flyway Migration Compatibility

**Contract**: All migrations must work on both PostgreSQL and H2 (after syntax updates)

**Supported Features**:
- CREATE TABLE with standard types (VARCHAR, TEXT, INTEGER)
- PRIMARY KEY, UNIQUE, NOT NULL constraints
- FOREIGN KEY with CASCADE/RESTRICT
- CHECK constraints for enum validation
- ALTER TABLE ADD COLUMN
- Basic DDL operations

**Unsupported in H2** (must avoid):
- PostgreSQL-specific types (JSONB, ARRAY, HSTORE)
- COMMENT ON statements (documentation only)
- PostgreSQL-specific functions (string_agg, array_agg, etc.)
- Full-text search (tsvector, tsquery)

**Migration Promise**: All existing migrations (V1, V2) work after minimal syntax updates (SERIAL → AUTO_INCREMENT).

## Summary

**API Surface**: No changes - all endpoints remain identical
**Repository Contracts**: Method signatures unchanged, implementation improved for portability
**Data Contracts**: YAML schema unchanged
**Test Contracts**: All tests pass without modification (after repository updates)
**Migration Contracts**: Flyway migrations work after syntax adjustments

**Breaking Changes**: None
**Deprecated Features**: None
**New Features**: None (infrastructure change only)

This is a **zero-impact migration** from API consumer perspective.
