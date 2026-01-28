# Data Model: Split YAML File Structure

**Feature**: 006-competencies-split
**Date**: 2026-01-28
**Status**: Phase 1 Design

## File Organization

```
src/main/resources/seed/
├── categories/
│   ├── programming.yaml
│   ├── software-design.yaml
│   ├── devops-infrastructure.yaml
│   ├── quality-testing.yaml
│   ├── soft-skills.yaml
│   └── architecture-frameworks.yaml
├── roles/
│   ├── junior-developer.yaml
│   ├── medior-developer.yaml
│   ├── senior-developer.yaml
│   ├── specialist-developer.yaml
│   ├── lead-developer.yaml
│   ├── lead-developer-software-architect.yaml
│   ├── software-architect.yaml
│   ├── solution-architect.yaml
│   └── devops-engineer.yaml
└── progressions.yaml  (optional: if progressions need splitting)
```

**Key Principle**: Each file contains ONE entity (one category or one role), not a wrapper structure.

---

## File Naming Convention

**Transformation Rule**: Display Name → Filename

```
Algorithm:
1. Convert to lowercase
2. Replace spaces with hyphens
3. Remove special characters (except hyphens)
4. Collapse multiple hyphens to single hyphen
5. Trim leading/trailing hyphens
6. Append .yaml extension
```

**Examples**:

| Display Name | Filename |
|--------------|----------|
| Programming | `programming.yaml` |
| Software Design | `software-design.yaml` |
| DevOps & Infrastructure | `devops-infrastructure.yaml` |
| Quality & Testing | `quality-testing.yaml` |
| Junior Developer | `junior-developer.yaml` |
| Lead Developer / Software Architect | `lead-developer-software-architect.yaml` |

**Uniqueness Guarantee**: The algorithm ensures no filename collisions for current data set. Future additions must verify uniqueness.

---

## Category File Schema

**File**: `categories/{category-name}.yaml`

**Structure**: Direct entity (NOT wrapped in a "category:" key)

```yaml
name: "Programming"
displayOrder: 1
skills:
  - name: "Java"
    levels:
      basic: "Kan basis Java code schrijven, begrijpt syntax en eenvoudige datastructuren"
      decent: "Schrijft schone code, begrijpt OOP en functionele principes, gebruikt collections effectief"
      good: "Ontwikkelt onderhoudbare systemen, is bedreven met streams en generics"
      excellent: "Beheerst geavanceerde patronen, draagt bij aan architectonische beslissingen"

  - name: "Python"
    levels:
      basic: "Kan scripts en eenvoudige programma's schrijven"
      decent: "Gebruikt standaardbibliotheek effectief, schrijft leesbare code"
      good: "Bouw applicaties met frameworks, begrijpt async patronen"
      excellent: "Creëert bibliotheken, optimaliseert performance, mentort anderen"

  # ... more skills
```

**Field Definitions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Category display name (must be unique across all category files) |
| `displayOrder` | Integer | No | Sort order for UI display (optional) |
| `skills` | List<Skill> | Yes | List of skills in this category (may be empty) |
| `skills[].name` | String | Yes | Skill display name (must be unique within category) |
| `skills[].levels` | Map<String, String> | Yes | Proficiency level descriptions |
| `skills[].levels.basic` | String | Yes | Basic proficiency description |
| `skills[].levels.decent` | String | Yes | Decent proficiency description |
| `skills[].levels.good` | String | Yes | Good proficiency description |
| `skills[].levels.excellent` | String | Yes | Excellent proficiency description |

**Validation Rules** (enforced by existing `validateYaml()`):
- Category name must not be empty
- All four proficiency levels must be present for each skill
- Skill names must be unique within the category

---

## Role File Schema

**File**: `roles/{role-name}.yaml`

**Structure**: Direct entity (NOT wrapped in a "role:" key)

```yaml
name: "Junior Developer"
description: "Entry-level software developer learning foundational programming skills"
roleFamily: "Development"
seniorityOrder: 1
requirements:
  - skillName: "Java"
    categoryName: "Programming"
    level: "basic"

  - skillName: "Git"
    categoryName: "Programming"
    level: "basic"

  - skillName: "Test-Driven Development"
    categoryName: "Software Design"
    level: "basic"

  # ... more requirements
```

**Field Definitions**:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | String | Yes | Role display name (must be unique across all role files) |
| `description` | String | Yes | Role purpose and responsibilities |
| `roleFamily` | String | No | Role category (e.g., "Development", "Architecture") |
| `seniorityOrder` | Integer | No | Seniority ranking (1 = junior, higher = senior) |
| `requirements` | List<Requirement> | Yes | Skills required for this role (may be empty) |
| `requirements[].skillName` | String | Yes | Skill name (must exist in some category) |
| `requirements[].categoryName` | String | Yes | Category name (must exist) |
| `requirements[].level` | String | Yes | Required proficiency level (basic/decent/good/excellent) |

**Validation Rules** (enforced by existing `validateYaml()`):
- Role name must not be empty
- Each requirement must reference an existing skill in an existing category
- Proficiency level must be one of: basic, decent, good, excellent

---

## Progressions Schema (Optional)

**File**: `progressions.yaml` (single file, or split further if needed)

**Structure**: List of progression relationships

```yaml
progressions:
  - fromRoleName: "Junior Developer"
    toRoleName: "Medior Developer"

  - fromRoleName: "Medior Developer"
    toRoleName: "Senior Developer"

  # ... more progressions
```

**Alternative** (if splitting progressions):
Create `progressions/{from-role}-to-{to-role}.yaml` files, each containing:

```yaml
fromRoleName: "Junior Developer"
toRoleName: "Medior Developer"
```

**Recommendation**: Keep progressions in a single file unless it exceeds 100 lines. Current data set has ~9 progressions, well under threshold.

---

## In-Memory Data Model (Unchanged)

**Java Record**: `YamlCompetencyData` (from `YamlCompetencyData.java`)

```java
public record YamlCompetencyData(
    List<CategoryData> categories,
    List<RoleData> roles,
    List<ProgressionData> progressions
) {}
```

**Loading Flow**:
1. Discover all `.yaml` files in `categories/` directory → List<Path>
2. For each category file: parse → CategoryData → add to list
3. Discover all `.yaml` files in `roles/` directory → List<Path>
4. For each role file: parse → RoleData → add to list
5. Parse `progressions.yaml` → List<ProgressionData>
6. Construct `YamlCompetencyData(allCategories, allRoles, allProgressions)`

**Key Insight**: The in-memory structure is IDENTICAL to single-file loading. Only the loading mechanism changes.

---

## Migration Mapping

**From** (current single file):
```yaml
categories:
  - name: "Programming"
    skills: [...]
  - name: "Software Design"
    skills: [...]

roles:
  - name: "Junior Developer"
    requirements: [...]
  - name: "Medior Developer"
    requirements: [...]
```

**To** (split files):

**categories/programming.yaml**:
```yaml
name: "Programming"
skills: [...]
```

**categories/software-design.yaml**:
```yaml
name: "Software Design"
skills: [...]
```

**roles/junior-developer.yaml**:
```yaml
name: "Junior Developer"
requirements: [...]
```

**roles/medior-developer.yaml**:
```yaml
name: "Medior Developer"
requirements: [...]
```

**Transformation**: Extract each array element into its own file, removing the wrapper keys (`categories:`, `roles:`).

---

## Validation Strategy

**Per-File Validation** (during parsing):
- YAML syntax correctness (SnakeYAML parser)
- Schema conformance (existing `validateYaml()` method)
- Filename matches entity name (optional sanity check)

**Cross-File Validation** (after merging):
- No duplicate category names across all category files
- No duplicate role names across all role files
- All skill requirements reference existing categories and skills
- All progressions reference existing roles

**Error Reporting Format**:
```
ERROR: Duplicate category 'Programming' found in files:
  - src/main/resources/seed/categories/programming.yaml
  - src/main/resources/seed/categories/programming-2.yaml
Application startup failed.
```

---

## Backward Compatibility

**Existing Database Schema**: UNCHANGED

The database tables (`competency_category`, `skill`, `role`, `role_skill_requirement`, `role_progression`) remain identical. The split files are merged in-memory before database synchronization, so the database sees the same data structure.

**Existing Application Code**: UNCHANGED

All code consuming competency data (`CategoryRepository`, `SkillRepository`, `RoleRepository`) continues to work without modification. The data model in memory is identical to before.

**Testing Strategy**: Keep `competencies.yaml` for regression testing. Verify split files produce identical database state as single file.

---

## Summary

| Aspect | Decision |
|--------|----------|
| Directory structure | `categories/` and `roles/` subdirectories |
| File naming | Lowercase-hyphenated transformation of display name |
| File schema | Direct entity (no wrapper keys) |
| In-memory model | Identical to current `YamlCompetencyData` |
| Validation | Per-file + cross-file duplicate detection |
| Backward compatibility | 100% - database and app code unchanged |

**Next Phase**: Generate implementation tasks with TDD approach (`/speckit.tasks`).
