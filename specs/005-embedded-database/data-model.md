# Data Model: In-Memory Database Migration

**Feature**: 005-embedded-database
**Date**: 2026-01-27
**Status**: Complete (No Schema Changes)

## Overview

This feature migrates from external PostgreSQL to H2 in-memory database. **The data model itself remains unchanged** - only database engine configuration and minor SQL syntax adjustments are required.

## Entity Model

### Existing Entities (Unchanged)

All entities are implemented as immutable Java Records with no JPA annotations (JDBC-only):

#### CompetencyCategory
```java
record CompetencyCategory(
    Integer id,
    String name,
    int displayOrder
)
```

**Purpose**: Groups skills into categories (e.g., "Programming", "Soft Skills")
**Constraints**:
- `name` must be unique
- `displayOrder` determines sort order in UI

---

#### Skill
```java
record Skill(
    Integer id,
    String name,
    Integer categoryId,
    String basicDescription,
    String decentDescription,
    String goodDescription,
    String excellentDescription
)
```

**Purpose**: Individual competency with 4 proficiency level descriptions
**Relationships**:
- Belongs to one `CompetencyCategory` (foreign key: `categoryId`)
**Constraints**:
- `(name, categoryId)` must be unique (no duplicate skill names within a category)

---

#### Role
```java
record Role(
    Integer id,
    String name,
    String description,
    String roleFamily,      // "Developer", "Architect", "Operations"
    Integer seniorityOrder  // 1=Junior, 2=Medior, 3=Senior
)
```

**Purpose**: Career role/position definition
**Constraints**:
- `name` must be unique
- `roleFamily` groups roles for matrix display
- `seniorityOrder` determines progression within family

---

#### RoleSkillRequirement
```java
record RoleSkillRequirement(
    Integer id,
    Integer roleId,
    Integer skillId,
    String requiredLevel  // "BASIC", "DECENT", "GOOD", "EXCELLENT"
)
```

**Purpose**: Links roles to required skills with proficiency level
**Relationships**:
- Belongs to one `Role` (foreign key: `roleId`, CASCADE DELETE)
- Belongs to one `Skill` (foreign key: `skillId`, CASCADE DELETE)
**Constraints**:
- `(roleId, skillId)` must be unique (no duplicate requirements)
- `requiredLevel` must be one of: BASIC, DECENT, GOOD, EXCELLENT

---

#### RoleProgression
```java
record RoleProgression(
    Integer id,
    Integer fromRoleId,
    Integer toRoleId
)
```

**Purpose**: Defines career progression paths between roles
**Relationships**:
- `fromRoleId` references `Role` (CASCADE DELETE)
- `toRoleId` references `Role` (CASCADE DELETE)
**Constraints**:
- `(fromRoleId, toRoleId)` must be unique

---

#### ProficiencyLevel (Enum)
```java
enum ProficiencyLevel {
    BASIC, DECENT, GOOD, EXCELLENT
}
```

**Purpose**: Type-safe enumeration of skill proficiency levels
**Usage**: Validation in repositories and sync service

## Database Schema

### Schema Changes for H2 Compatibility

The schema structure remains identical, but SQL syntax requires minor adjustments:

#### Migration V1: SERIAL → AUTO_INCREMENT

**Before (PostgreSQL)**:
```sql
CREATE TABLE competency_category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL
);
```

**After (H2 Compatible)**:
```sql
CREATE TABLE competency_category (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL
);
```

**Impact**: 5 tables require this change (competency_category, rolename, skill, role_skill_requirement, role_progression)

#### Migration V2: Remove COMMENT ON Statements

**Before (PostgreSQL)**:
```sql
COMMENT ON COLUMN rolename.role_family IS 'Groups roles: Developer, Architect, Operations, Other';
COMMENT ON COLUMN rolename.seniority_order IS 'Ordering within family: 1=Junior, 2=Medior, 3=Senior';
```

**After (H2 Compatible)**:
```sql
-- H2 does not support COMMENT ON - remove these statements
-- Documentation moved to data-model.md and inline comments
```

**Impact**: 2 statements removed from V2 migration

### Foreign Key Constraints (Unchanged)

All foreign key constraints work identically in H2:

```sql
-- Example: RoleSkillRequirement foreign keys
CONSTRAINT fk_role_skill_role FOREIGN KEY (role_id)
    REFERENCES rolename(id)
    ON DELETE CASCADE,

CONSTRAINT fk_role_skill_skill FOREIGN KEY (skill_id)
    REFERENCES skill(id)
    ON DELETE CASCADE
```

### CHECK Constraints (Unchanged)

```sql
-- Example: ProficiencyLevel validation
CONSTRAINT check_required_level
    CHECK (required_level IN ('BASIC', 'DECENT', 'GOOD', 'EXCELLENT'))
```

## Data Access Patterns

### Repository Pattern (Minor Refactoring Required)

#### Current Approach (PostgreSQL-specific)
Uses `RETURNING` clause for insert operations:

```java
// RoleRepository.java - BEFORE
String sql = "INSERT INTO rolename (name, description, role_family, seniority_order)
              VALUES (?, ?, ?, ?) RETURNING id";
PreparedStatement stmt = conn.prepareStatement(sql);
// ... set parameters ...
ResultSet rs = stmt.executeQuery();
```

#### New Approach (H2 Compatible)
Uses JDBC `RETURN_GENERATED_KEYS`:

```java
// RoleRepository.java - AFTER
String sql = "INSERT INTO rolename (name, description, role_family, seniority_order)
              VALUES (?, ?, ?, ?)";
PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
// ... set parameters ...
int affectedRows = stmt.executeUpdate();
ResultSet generatedKeys = stmt.getGeneratedKeys();
if (generatedKeys.next()) {
    Integer id = generatedKeys.getInt(1);
    return new Role(id, name, description, roleFamily, seniorityOrder);
}
```

**Impact**: 2 repositories require this refactoring (RoleRepository, SkillRepository)

### Query Patterns (Unchanged)

All SELECT queries work identically:
- Standard SQL joins
- `LOWER()` function for case-insensitive search
- `ORDER BY` for sorting
- Parameterized queries via `PreparedStatement`

## Data Lifecycle

### Data Source of Truth: YAML

**File**: `src/main/resources/seed/competencies.yaml`

**Structure**:
```yaml
categories:
  - name: Programming
    displayOrder: 1
    skills:
      - name: Java
        basic: "Knows Java syntax..."
        decent: "Can write clean Java code..."
        good: "Designs maintainable systems..."
        excellent: "Architects complex solutions..."

roles:
  - name: Junior Developer
    description: "Entry-level software engineer"
    roleFamily: Developer
    seniorityOrder: 1
    requirements:
      - skill: Java
        level: BASIC
      - skill: Git
        level: BASIC
```

### Data Loading Workflow

```
Application Startup
    ↓
DataSeeder observes StartupEvent
    ↓
CompetencySyncService.syncFromConfiguration()
    ↓
Parse competencies.yaml (SnakeYAML)
    ↓
Validate YAML structure
    ↓
Execute sync mode (REPLACE, MERGE, or NONE)
    ↓
Insert data via repositories
    ↓
H2 in-memory database populated
    ↓
Application ready
```

**Sync Modes** (unchanged):
1. **REPLACE**: Delete all competency data, reload from YAML
2. **MERGE**: Update existing records, add new records, preserve unmatched DB records
3. **NONE**: Skip sync entirely

### Data Persistence

**H2 In-Memory Behavior**:
- Data lives only in application memory
- Data is destroyed on application shutdown
- Data is reloaded from YAML on each startup
- **YAML is the only persistent store**

**Benefits**:
- YAML changes immediately reflected on restart
- No database state drift between environments
- Git version control for all data changes
- Simplified backup (just version YAML file)

## Migration Path

### Step-by-Step Migration

1. **Update Flyway Migrations**:
   - V1: Replace SERIAL with AUTO_INCREMENT (5 tables)
   - V2: Remove COMMENT ON statements (2 lines)

2. **Refactor Repositories**:
   - RoleRepository: Replace RETURNING with generated keys API
   - SkillRepository: Replace RETURNING with generated keys API

3. **Update Configuration**:
   - pom.xml: Add quarkus-jdbc-h2 dependency
   - application.properties: Configure H2 datasource

4. **Verify with Tests**:
   - Run full test suite (all tests should pass)
   - Verify data integrity against YAML seed data

### Rollback Strategy

If H2 adoption encounters issues:
1. Revert pom.xml dependency changes
2. Revert application.properties datasource config
3. Revert migration files to SERIAL syntax
4. Revert repository methods to RETURNING clause
5. Resume using PostgreSQL

## Validation Rules

### Existing Validation (Unchanged)

**CompetencySyncService** validates:
- YAML structure completeness
- Required fields presence
- Data type correctness
- Referential integrity (skills reference valid categories)
- Proficiency level enum values

**Database Constraints** validate:
- UNIQUE constraints (no duplicate names)
- CHECK constraints (enum values)
- FOREIGN KEY constraints (referential integrity)
- NOT NULL constraints (required fields)

## Performance Considerations

### H2 In-Memory Characteristics

**Startup Performance**:
- Schema creation: <50ms (5 tables, Flyway migrations)
- Data loading: <100ms (~50 records from YAML)
- Total startup impact: Negligible (<150ms)

**Query Performance**:
- Small dataset (50 records): All queries <10ms
- No indexes needed (dataset fits in memory)
- Concurrent reads: Excellent (H2 uses MVCC)

**Memory Footprint**:
- Expected: <10MB for current dataset
- Growth: Linear with data size
- Acceptable for 100-1000 records

### Scalability Limits

**When to Consider External Database**:
- Dataset exceeds 10,000 records
- Multi-instance deployment needed
- Data persistence between restarts required
- Advanced PostgreSQL features needed (full-text search, JSON functions)

For current use case (development tool, small dataset), H2 in-memory is ideal.

## Summary of Changes

| Aspect | Before | After | Impact |
|--------|--------|-------|--------|
| Database Engine | PostgreSQL 18.1 | H2 (PostgreSQL mode) | Configuration change |
| Entity Models | Java Records | Java Records (unchanged) | None |
| Foreign Keys | CASCADE/RESTRICT | CASCADE/RESTRICT (unchanged) | None |
| CHECK Constraints | Enum validation | Enum validation (unchanged) | None |
| INSERT Syntax | RETURNING clause | Generated keys API | 2 repositories |
| Schema DDL | SERIAL type | AUTO_INCREMENT | 2 migration files |
| Comments | COMMENT ON | Removed | 2 statements |
| YAML Workflow | CompetencySyncService | CompetencySyncService (unchanged) | None |
| Data Persistence | PostgreSQL disk | H2 memory (YAML source) | Architecture change |

**Total Code Impact**: Minimal
- 2 migration files modified
- 2 repository methods refactored
- 1 dependency change (pom.xml)
- 3 configuration lines (application.properties)

**Total Functional Impact**: Zero
- All features work identically
- All tests should pass without modification
- UI unchanged
- API unchanged
