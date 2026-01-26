# Research & Technical Decisions: Competencies Data Synchronization

**Feature**: 003-competencies-sync
**Date**: 2026-01-26
**Status**: Complete

## Overview

This document captures research findings and technical decisions made during the planning phase for the competencies data synchronization feature.

## Decision 1: Sync Mode Configuration Strategy

**Decision**: Use Eclipse MicroProfile Config with `@ConfigProperty` injection and enum-based sync mode

**Rationale**:
- Quarkus provides built-in configuration injection via `@ConfigProperty`
- Enum ensures type safety and prevents invalid values at compile time (after startup validation)
- No additional dependencies required
- Follows Quarkus best practices for configuration management
- Default value support handles missing property case cleanly

**Alternatives Considered**:
- **Properties file parsing manually**: Rejected because Quarkus Config provides superior validation and type conversion
- **Environment variable only**: Rejected because application.properties is the standard Quarkus configuration location
- **String-based mode with validation**: Rejected because enum provides compile-time safety

**Implementation**:
```java
@ConfigProperty(name = "competency.sync.mode", defaultValue = "none")
SyncMode syncMode;
```

**Configuration Property**:
```properties
# application.properties
competency.sync.mode=merge  # Options: none, merge, replace
```

## Decision 2: Case/Space-Insensitive Matching Strategy

**Decision**: Normalize strings to lowercase and collapse spaces for matching, but preserve original casing when storing

**Rationale**:
- Java's `String.toLowerCase()` + `replaceAll("\\s+", " ").trim()` provides simple normalization
- Storing original casing from YAML preserves administrator intent
- No database collation changes needed (matching happens in Java code)
- Simplicity First principle: no need for complex fuzzy matching libraries

**Alternatives Considered**:
- **PostgreSQL case-insensitive collation**: Rejected because it requires schema migration and affects all queries
- **Apache Commons StringUtils**: Rejected because it's overkill for simple normalization
- **Case-sensitive exact match**: Rejected because spec requires case-insensitive matching

**Implementation Pattern**:
```java
private String normalize(String value) {
    return value.trim().replaceAll("\\s+", " ").toLowerCase();
}

// Usage
Optional<Skill> findSkill(String name, Long categoryId) {
    String normalizedName = normalize(name);
    return skillRepository.list("LOWER(TRIM(name)) = ?1 AND categoryId = ?2",
                                 normalizedName, categoryId)
                          .stream()
                          .findFirst();
}
```

## Decision 3: Transaction Management Strategy

**Decision**: Use existing `@Transactional` annotation at method level on sync orchestrator method

**Rationale**:
- Quarkus provides declarative transaction management via JTA
- Single transaction ensures atomicity: all changes succeed or all roll back
- No need for manual transaction handling or savepoints
- Existing DataSeeder already uses this pattern successfully
- Simplicity First: leverage framework capabilities

**Alternatives Considered**:
- **Manual transaction management**: Rejected as unnecessarily complex
- **Multiple transactions per entity type**: Rejected because partial success violates Data Integrity principle
- **Two-phase commit**: Rejected as overkill for single-database operation

**Implementation**:
```java
@Transactional
public void syncCompetencies(SyncMode mode) {
    // All sync logic here - single transaction boundary
}
```

## Decision 4: YAML Validation Approach

**Decision**: Validate YAML structure immediately after parsing, before any database operations

**Rationale**:
- Fail-fast approach aligns with Data Integrity principle
- SnakeYAML parsing exceptions caught and re-thrown with clear messages
- Structure validation checks required keys (categories, skills, roles) exist
- Prevents partial database updates from malformed data

**Alternatives Considered**:
- **Schema validation with JSON Schema**: Rejected as adding unnecessary dependency (no YAML schema validator in Quarkus)
- **Validate during database operations**: Rejected because it could leave database in inconsistent state
- **Skip validation**: Rejected as violating Data Integrity principle

**Validation Checks**:
1. YAML syntax valid (SnakeYAML parsing succeeds)
2. Top-level keys present: `categories`, `roles`, `progressions`
3. Each category has `name` and `skills` list
4. Each skill has `name` and `levels` map with basic/decent/good/excellent
5. Each role has `name`, `description`, and `requirements` list
6. Each requirement has `skill`, `category`, and `level`

## Decision 5: Logging Strategy

**Decision**: Use SLF4J with structured logging pattern: individual operation logs + aggregate summary

**Rationale**:
- SLF4J already used throughout codebase (via Quarkus logging)
- INFO level for successful operations (audit trail)
- ERROR level for failures (alerting)
- Aggregate summary logged at end provides at-a-glance status
- Follows observability best practices

**Alternatives Considered**:
- **Detailed DEBUG only**: Rejected because administrators need INFO-level visibility
- **Summary only**: Rejected because spec requires individual change logging
- **Structured logging library (Logstash)**: Rejected as premature optimization

**Log Format Examples**:
```
INFO  [DataSeeder] Starting competency sync - mode: merge
INFO  [DataSeeder] Category synced: Programming
INFO  [DataSeeder] Skill synced: Java in category Programming
INFO  [DataSeeder] Role synced: Senior Developer
INFO  [DataSeeder] Sync complete: 3 categories updated, 12 skills added, 5 roles updated, 2 progressions added
```

## Decision 6: Dependency Resolution Order

**Decision**: Process entities in dependency order: Categories → Skills → Roles → Requirements → Progressions

**Rationale**:
- Respects foreign key constraints (skills depend on categories, requirements depend on skills)
- When role requirement references missing skill, check YAML for skill definition first
- If skill exists in YAML (case-insensitive match), create it immediately
- If skill not found in either database or YAML, fail with ERROR log
- Simplicity First: linear processing order, no complex dependency graph

**Alternatives Considered**:
- **Topological sort**: Rejected as overengineering for fixed hierarchy
- **Skip missing references**: Rejected as violating referential integrity
- **Create placeholder entities**: Rejected as creating invalid data

**Processing Order**:
1. Parse entire YAML into memory structures
2. Build lookup maps for case-insensitive matching
3. Process categories (create/update)
4. Process skills (create/update, referencing category map)
5. Process roles (create/update)
6. Process requirements (create/update, checking skill existence in both DB and YAML)
7. Process progressions (create/update, referencing role map)

## Decision 7: Repository Method Enhancements

**Decision**: Add case-insensitive query methods to existing repositories rather than changing database schema

**Rationale**:
- No migration needed
- Backward compatible with existing queries
- Follows Repository pattern already in use
- Query methods can use PostgreSQL's LOWER() function for case-insensitive matching

**New Methods Needed**:
```java
// CategoryRepository
Optional<CompetencyCategory> findByNameIgnoreCase(String name);

// SkillRepository
Optional<Skill> findByNameAndCategoryIdIgnoreCase(String name, Long categoryId);

// RoleRepository
Optional<Role> findByNameIgnoreCase(String name);
```

## Decision 8: Merge Mode Update Detection

**Decision**: Compare all entity attributes to determine if update is needed

**Rationale**:
- Avoid unnecessary database writes (performance optimization per FR-014)
- Simple field-by-field comparison using equals()
- Update only if ANY field differs from current database value
- Preserves database audit timestamps (updated_at) by only writing when necessary

**Alternatives Considered**:
- **Always update on match**: Rejected as inefficient (unnecessary writes)
- **Checksum-based comparison**: Rejected as premature optimization
- **Skip update detection**: Rejected as violating FR-014 efficiency requirement

**Comparison Logic**:
```java
boolean needsUpdate(CompetencyCategory existing, CompetencyCategory yaml) {
    return !Objects.equals(existing.name(), yaml.name()) ||
           existing.displayOrder() != yaml.displayOrder();
}
```

## Decision 9: Replace Mode Implementation

**Decision**: Delete all entities in reverse dependency order, then seed fresh from YAML

**Rationale**:
- Clean slate approach: no orphaned data
- Reverse order respects foreign key constraints
- Reuse existing seed logic after deletion
- Transaction rollback protects against partial deletion

**Deletion Order**:
1. RoleProgressions (no dependencies)
2. RoleSkillRequirements (depends on roles and skills)
3. Roles (no dependencies after requirements deleted)
4. Skills (depends on categories)
5. CompetencyCategories (no dependencies after skills deleted)

**Alternatives Considered**:
- **Soft delete**: Rejected as not in scope
- **Cascade delete from categories**: Rejected because explicit order is clearer
- **Truncate tables**: Rejected because @Transactional works better with DELETE

## Decision 10: None Mode Implementation

**Decision**: Skip all sync logic when mode is "none", log at WARNING level if property missing

**Rationale**:
- Simplest implementation: early return
- Preserves existing behavior when property not set (default to "none")
- WARNING log alerts administrators to missing configuration
- No database queries executed

**Implementation**:
```java
if (syncMode == SyncMode.NONE) {
    if (configPropertyExplicitlySet) {
        log.info("Competency sync disabled (mode: none)");
    } else {
        log.warn("Competency sync mode not configured, defaulting to 'none'. " +
                 "Set competency.sync.mode property to 'merge' or 'replace' to enable sync.");
    }
    return;
}
```

## Technology Stack Summary

**No new dependencies required** - all capabilities exist in current stack:

- **Configuration**: Eclipse MicroProfile Config (Quarkus built-in)
- **YAML Parsing**: SnakeYAML (already in project)
- **Transactions**: JTA via `@Transactional` (Quarkus built-in)
- **Logging**: SLF4J (Quarkus built-in)
- **Database Access**: Panache JDBC (existing)
- **Testing**: JUnit 5 + Quarkus Test + Testcontainers (existing)

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Large YAML file causes memory issues | YAML parsing loads entire file into memory, but 100 skills + 20 roles is <1MB - acceptable |
| Sync timeout on large dataset | 5-second performance goal is well within Quarkus startup time budget; tested with Testcontainers |
| Case-insensitive matching false positives | Space normalization reduces risk; comprehensive test coverage for edge cases |
| Transaction timeout on slow database | Default Quarkus transaction timeout (5 minutes) is sufficient; can be configured if needed |

## Next Steps

1. **Phase 1**: Create data-model.md defining sync state representation
2. **Phase 1**: Generate contracts/ (none needed - internal feature)
3. **Phase 1**: Create quickstart.md for developer setup
4. **Phase 2**: Generate tasks.md breaking down implementation work
