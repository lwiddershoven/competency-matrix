# Data Model: Competencies Data Synchronization

**Feature**: 003-competencies-sync
**Date**: 2026-01-26
**Status**: Complete

## Overview

This document describes the data model for the competencies synchronization feature. The feature does not introduce new persistent entities but does introduce transient data structures for sync orchestration and configuration.

## Existing Database Schema

**No changes to existing database schema required.** The sync feature operates on existing entities:

```sql
-- Existing tables (no modifications)
competency_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    display_order INTEGER NOT NULL
);

skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL REFERENCES competency_categories(id),
    basic_description TEXT,
    decent_description TEXT,
    good_description TEXT,
    excellent_description TEXT
);

roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT
);

role_skill_requirements (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL REFERENCES roles(id),
    skill_id BIGINT NOT NULL REFERENCES skills(id),
    level VARCHAR(20) NOT NULL -- BASIC, DECENT, GOOD, EXCELLENT
);

role_progressions (
    id BIGSERIAL PRIMARY KEY,
    from_role_id BIGINT NOT NULL REFERENCES roles(id),
    to_role_id BIGINT NOT NULL REFERENCES roles(id)
);
```

## New Configuration Model

### SyncMode Enum

**Purpose**: Type-safe representation of sync behavior modes

**Values**:
- `NONE`: No synchronization performed
- `MERGE`: Incremental updates (add new, update existing, preserve rest)
- `REPLACE`: Full replacement (delete all, seed fresh)

**Implementation**:
```java
package nl.leonw.competencymatrix.config;

public enum SyncMode {
    NONE,    // Skip all sync operations
    MERGE,   // Add new + update existing + preserve unchanged
    REPLACE  // Delete all + seed fresh from YAML
}
```

**Configuration Property**:
```properties
# application.properties
competency.sync.mode=merge  # Options: none, merge, replace (case-insensitive)
```

## Transient Data Structures

These structures exist only during sync processing, not persisted to database.

### YamlCompetencyData

**Purpose**: In-memory representation of parsed YAML file

**Structure**:
```java
package nl.leonw.competencymatrix.config;

public record YamlCompetencyData(
    List<CategoryData> categories,
    List<RoleData> roles,
    List<ProgressionData> progressions
) {
    public record CategoryData(
        String name,
        int displayOrder,
        List<SkillData> skills
    ) {}

    public record SkillData(
        String name,
        String categoryName,
        Map<String, String> levels  // basic, decent, good, excellent
    ) {}

    public record RoleData(
        String name,
        String description,
        List<RequirementData> requirements
    ) {}

    public record RequirementData(
        String skillName,
        String categoryName,
        String level  // BASIC, DECENT, GOOD, EXCELLENT
    ) {}

    public record ProgressionData(
        String fromRoleName,
        String toRoleName
    ) {}
}
```

### SyncResult

**Purpose**: Track sync operation outcomes for logging and reporting

**Structure**:
```java
package nl.leonw.competencymatrix.config;

public record SyncResult(
    int categoriesAdded,
    int categoriesUpdated,
    int skillsAdded,
    int skillsUpdated,
    int rolesAdded,
    int rolesUpdated,
    int requirementsAdded,
    int requirementsUpdated,
    int progressionsAdded,
    int progressionsUpdated,
    int categoriesDeleted,    // Only non-zero in REPLACE mode
    int skillsDeleted,        // Only non-zero in REPLACE mode
    int rolesDeleted,         // Only non-zero in REPLACE mode
    int requirementsDeleted,  // Only non-zero in REPLACE mode
    int progressionsDeleted   // Only non-zero in REPLACE mode
) {
    public String formatSummary() {
        StringBuilder sb = new StringBuilder("Sync complete: ");

        if (categoriesAdded + categoriesUpdated > 0) {
            sb.append(String.format("%d categories (%d added, %d updated), ",
                      categoriesAdded + categoriesUpdated, categoriesAdded, categoriesUpdated));
        }
        if (skillsAdded + skillsUpdated > 0) {
            sb.append(String.format("%d skills (%d added, %d updated), ",
                      skillsAdded + skillsUpdated, skillsAdded, skillsUpdated));
        }
        if (rolesAdded + rolesUpdated > 0) {
            sb.append(String.format("%d roles (%d added, %d updated), ",
                      rolesAdded + rolesUpdated, rolesAdded, rolesUpdated));
        }
        if (requirementsAdded + requirementsUpdated > 0) {
            sb.append(String.format("%d requirements (%d added, %d updated), ",
                      requirementsAdded + requirementsUpdated, requirementsAdded, requirementsUpdated));
        }
        if (progressionsAdded + progressionsUpdated > 0) {
            sb.append(String.format("%d progressions (%d added, %d updated), ",
                      progressionsAdded + progressionsUpdated, progressionsAdded, progressionsUpdated));
        }

        // Remove trailing comma and space
        if (sb.length() > 15) {
            sb.setLength(sb.length() - 2);
        } else {
            sb.append("no changes");
        }

        return sb.toString();
    }
}
```

## Entity Matching Rules

### Case/Space-Insensitive Matching

All entity name comparisons use normalization:

```java
private String normalize(String value) {
    if (value == null) return "";
    return value.trim().replaceAll("\\s+", " ").toLowerCase();
}
```

**Matching Logic**:
- **Categories**: Match by `normalize(name)`
- **Skills**: Match by `normalize(name)` AND `categoryId` (skills can have same name in different categories)
- **Roles**: Match by `normalize(name)`
- **Requirements**: Match by `roleId` AND `skillId` (unique constraint)
- **Progressions**: Match by `fromRoleId` AND `toRoleId` (unique constraint)

**Storage Rule**: Original casing from YAML is preserved when creating or updating entities.

### Comparison for Updates

Entities are updated only if ANY field differs from database value:

**Category Update Check**:
```java
boolean needsUpdate = !existing.name().equals(yaml.name()) ||
                      existing.displayOrder() != yaml.displayOrder();
```

**Skill Update Check**:
```java
boolean needsUpdate = !existing.name().equals(yaml.name()) ||
                      !Objects.equals(existing.basicDescription(), yaml.basicDescription()) ||
                      !Objects.equals(existing.decentDescription(), yaml.decentDescription()) ||
                      !Objects.equals(existing.goodDescription(), yaml.goodDescription()) ||
                      !Objects.equals(existing.excellentDescription(), yaml.excellentDescription());
```

**Role Update Check**:
```java
boolean needsUpdate = !existing.name().equals(yaml.name()) ||
                      !Objects.equals(existing.description(), yaml.description());
```

**Requirements/Progressions**: Always replaced (deleted and recreated) if role/skill composition changes.

## Processing Order

Entities are processed in dependency order to maintain referential integrity:

```text
MERGE mode:
1. Parse YAML → YamlCompetencyData
2. Validate structure
3. Process Categories (create new, update existing)
4. Process Skills (create new, update existing)
5. Process Roles (create new, update existing)
6. Process Requirements (create new, update existing)
7. Process Progressions (create new, update existing)
8. Build and log SyncResult

REPLACE mode:
1. Parse YAML → YamlCompetencyData
2. Validate structure
3. Delete all existing data (reverse dependency order):
   - RoleProgressions
   - RoleSkillRequirements
   - Roles
   - Skills
   - CompetencyCategories
4. Seed fresh from YAML (same order as MERGE)
5. Build and log SyncResult

NONE mode:
1. Log warning if property not set, log info if explicitly set to none
2. Return immediately (no database operations)
```

## Validation Rules

### YAML Structure Validation

Performed immediately after parsing, before any database operations:

1. **Top-level keys exist**: `categories`, `roles`, `progressions`
2. **Category structure**:
   - `name` present and non-empty
   - `skills` is a list (may be empty)
3. **Skill structure**:
   - `name` present and non-empty
   - `levels` is a map containing keys: `basic`, `decent`, `good`, `excellent`
   - All level descriptions non-empty
4. **Role structure**:
   - `name` present and non-empty
   - `description` present
   - `requirements` is a list (may be empty)
5. **Requirement structure**:
   - `skill` present and non-empty
   - `category` present and non-empty
   - `level` present and one of: `basic`, `decent`, `good`, `excellent`
6. **Progression structure**:
   - `from` present and non-empty
   - `to` present and non-empty

### Referential Integrity Validation

Performed during sync processing:

1. **Skill category reference**: When processing skill, ensure category exists (either in DB or in YAML categories list)
2. **Requirement skill reference**: When processing requirement, ensure skill exists (check DB first, then YAML skills list with case-insensitive match)
3. **Progression role references**: When processing progression, ensure both from_role and to_role exist (check DB first, then YAML roles list with case-insensitive match)

**Failure Behavior**: Log ERROR with specific missing reference details and fail startup (rollback transaction).

## Error Handling

### Validation Failures

**Trigger**: YAML syntax error, missing required fields, invalid structure

**Behavior**:
- Log ERROR with clear message indicating specific validation failure
- Example: `"Invalid YAML: category 'Programming' is missing required 'skills' field"`
- Throw RuntimeException to fail startup
- Transaction rollback (no database changes)

### Referential Integrity Failures

**Trigger**: Role requirement references skill not in DB or YAML

**Behavior**:
- Log ERROR with specific details
- Example: `"Role 'Senior Developer' references skill 'Kotlin' in category 'Programming' which does not exist (case-insensitive) in database or YAML"`
- Throw RuntimeException to fail startup
- Transaction rollback (no database changes)

### Database Operation Failures

**Trigger**: Constraint violation, connection error, query error

**Behavior**:
- Log ERROR with database error details
- Example: `"Sync failed: duplicate key violation on competency_categories.name"`
- Throw RuntimeException to fail startup
- Transaction rollback (no database changes)

### Invalid Configuration

**Trigger**: `competency.sync.mode` property set to value other than `none`, `merge`, `replace`

**Behavior**:
- Log ERROR with clear message listing valid values
- Example: `"Invalid competency.sync.mode value 'sync': must be one of [none, merge, replace]"`
- Throw RuntimeException to fail startup

## Observability

### Logging Events

**Level: INFO** (successful operations):
- Sync start: `"Starting competency sync - mode: {mode}"`
- Per-entity operations: `"Category synced: {name}"`, `"Skill synced: {name} in category {category}"`
- Sync complete: `"{SyncResult.formatSummary()}"`

**Level: WARN** (non-fatal issues):
- Missing configuration property: `"Competency sync mode not configured, defaulting to 'none'. Set competency.sync.mode property."`

**Level: ERROR** (failures):
- Validation failures: `"Invalid YAML: {specific error}"`
- Referential integrity failures: `"Role '{role}' references skill '{skill}' which does not exist in database or YAML"`
- Database errors: `"Sync failed: {error details}"`
- Configuration errors: `"Invalid competency.sync.mode value '{value}': must be one of [none, merge, replace]"`

### Performance Metrics

Captured implicitly via Quarkus startup time metrics:
- Sync duration included in application startup time
- Goal: <5 seconds for 100 skills + 20 roles
- Monitored via Quarkus health checks and startup logs

## Repository Enhancements

### New Query Methods

**CategoryRepository**:
```java
Optional<CompetencyCategory> findByNameIgnoreCase(String name) {
    return find("LOWER(TRIM(name)) = ?1", name.trim().toLowerCase()).firstResultOptional();
}
```

**SkillRepository**:
```java
Optional<Skill> findByNameAndCategoryIdIgnoreCase(String name, Long categoryId) {
    return find("LOWER(TRIM(name)) = ?1 AND categoryId = ?2",
                name.trim().toLowerCase(), categoryId).firstResultOptional();
}
```

**RoleRepository**:
```java
Optional<Role> findByNameIgnoreCase(String name) {
    return find("LOWER(TRIM(name)) = ?1", name.trim().toLowerCase()).firstResultOptional();
}
```

### Delete Methods for REPLACE Mode

**RoleSkillRequirementRepository**:
```java
@Transactional
void deleteAll() {
    delete("1=1");  // Deletes all requirements
}
```

**RoleProgressionRepository**:
```java
@Transactional
void deleteAll() {
    delete("1=1");  // Deletes all progressions
}
```

Similar `deleteAll()` methods for other repositories (leveraging existing Panache patterns).

## State Transitions

### Sync Mode State Machine

```text
Application Startup
        |
        v
Read competency.sync.mode property
        |
        +-- NONE --> Log warning/info --> Return (no DB operations)
        |
        +-- MERGE --> Parse YAML --> Validate --> Sync Merge --> Log Summary
        |
        +-- REPLACE --> Parse YAML --> Validate --> Delete All --> Seed Fresh --> Log Summary
        |
        +-- INVALID --> Log ERROR --> Fail Startup
```

### Entity Lifecycle (MERGE Mode)

```text
For each entity in YAML:
    |
    v
Normalize name for matching
    |
    v
Query DB with case-insensitive match
    |
    +-- Not Found --> CREATE new entity --> Log "added" --> Increment counter
    |
    +-- Found --> Compare fields
                    |
                    +-- Differs --> UPDATE entity --> Log "updated" --> Increment counter
                    |
                    +-- Same --> Skip (no DB write)
```

### Entity Lifecycle (REPLACE Mode)

```text
1. Delete Phase (reverse dependency order):
   - Delete all RoleProgressions
   - Delete all RoleSkillRequirements
   - Delete all Roles
   - Delete all Skills
   - Delete all CompetencyCategories

2. Seed Phase (dependency order):
   - Create all Categories from YAML
   - Create all Skills from YAML
   - Create all Roles from YAML
   - Create all Requirements from YAML
   - Create all Progressions from YAML
```

All operations within single transaction - rollback on any failure.
