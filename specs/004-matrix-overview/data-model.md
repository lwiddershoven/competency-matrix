# Data Model: Matrix Overview

**Feature**: 004-matrix-overview
**Date**: 2026-01-26

## Overview

This document defines the data structures (DTOs) used to render the competency matrix table. The matrix reuses existing domain entities (Skill, Role, RoleSkillRequirement) but introduces view-specific DTOs to optimize rendering and support filtering.

## View Models (DTOs)

### MatrixViewModel

Top-level DTO containing all data needed to render the matrix page.

```java
package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.CompetencyCategory;
import java.util.List;
import java.util.Map;

/**
 * Complete view model for the matrix overview page.
 */
public record MatrixViewModel(
    List<MatrixRow> rows,
    Map<String, List<MatrixColumnHeader>> rolesByFamily,
    List<CompetencyCategory> categories,
    String selectedCategoryId
) {
    /**
     * Create matrix view with no category filter applied.
     */
    public static MatrixViewModel unfiltered(
        List<MatrixRow> rows,
        Map<String, List<MatrixColumnHeader>> rolesByFamily,
        List<CompetencyCategory> categories
    ) {
        return new MatrixViewModel(rows, rolesByFamily, categories, null);
    }

    /**
     * Create matrix view filtered by category.
     */
    public static MatrixViewModel filtered(
        List<MatrixRow> rows,
        Map<String, List<MatrixColumnHeader>> rolesByFamily,
        List<CompetencyCategory> categories,
        String categoryId
    ) {
        return new MatrixViewModel(rows, rolesByFamily, categories, categoryId);
    }

    public boolean hasFilter() {
        return selectedCategoryId != null && !selectedCategoryId.isEmpty();
    }
}
```

### MatrixRow

Represents a single skill row in the matrix.

```java
package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Skill;
import java.util.List;

/**
 * A single row in the matrix representing one skill across all roles.
 */
public record MatrixRow(
    Skill skill,
    List<MatrixCell> cells
) {
    /**
     * Get cell for specific role ID.
     * Returns empty cell if no requirement exists.
     */
    public MatrixCell getCellForRole(Integer roleId) {
        return cells.stream()
            .filter(cell -> cell.roleId().equals(roleId))
            .findFirst()
            .orElse(MatrixCell.empty(skill.id(), roleId));
    }
}
```

### MatrixCell

Represents a single cell in the matrix (skill-role intersection).

```java
package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.ProficiencyLevel;

/**
 * A single cell in the matrix showing proficiency requirement.
 */
public record MatrixCell(
    Long skillId,
    Integer roleId,
    ProficiencyLevel requiredLevel,
    boolean isEmpty
) {
    /**
     * Create cell with proficiency requirement.
     */
    public static MatrixCell withLevel(Long skillId, Integer roleId, ProficiencyLevel level) {
        return new MatrixCell(skillId, roleId, level, false);
    }

    /**
     * Create empty cell (no proficiency requirement for this skill-role combination).
     */
    public static MatrixCell empty(Long skillId, Integer roleId) {
        return new MatrixCell(skillId, roleId, null, true);
    }

    /**
     * Get CSS class for level badge.
     */
    public String getLevelCssClass() {
        if (isEmpty) return "";
        return "level-" + requiredLevel.name().toLowerCase();
    }

    /**
     * Get display text for cell.
     */
    public String getDisplayText() {
        if (isEmpty) return "";
        return requiredLevel.displayName();
    }
}
```

### MatrixColumnHeader

Represents a role column header with metadata for rendering.

```java
package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Role;

/**
 * Column header for a role in the matrix.
 */
public record MatrixColumnHeader(
    Role role,
    String abbreviation
) {
    /**
     * Create column header from role.
     * Generates abbreviation if role name is too long.
     */
    public static MatrixColumnHeader from(Role role) {
        String abbrev = generateAbbreviation(role.name());
        return new MatrixColumnHeader(role, abbrev);
    }

    private static String generateAbbreviation(String name) {
        // "Junior Developer" → "Jr Dev"
        // "Software Architect" → "SW Arch"
        if (name.length() <= 15) {
            return name;
        }

        return name.chars()
            .mapToObj(c -> (char) c)
            .filter(Character::isUpperCase)
            .limit(3)
            .map(String::valueOf)
            .reduce("", String::concat);
    }

    /**
     * Whether to show tooltip with full name.
     */
    public boolean needsTooltip() {
        return role.name().length() > 15;
    }
}
```

## Domain Model Updates

### Role Entity (Extended)

Add new fields for grouping and ordering:

```java
package nl.leonw.competencymatrix.model;

/**
 * Career role with seniority level embedded in name.
 * UPDATED: Added roleFamily and seniorityOrder for matrix grouping.
 */
public record Role(
    Integer id,
    String name,
    String description,
    String roleFamily,       // NEW: "Developer", "Architect", "Operations"
    Integer seniorityOrder   // NEW: 1=Junior, 2=Medior, 3=Senior, etc.
) {
    public Role(String name, String description, String roleFamily, Integer seniorityOrder) {
        this(null, name, description, roleFamily, seniorityOrder);
    }
}
```

**Migration Required**: `V2__add_role_grouping.sql` (see research.md)

## Data Flow

```
┌─────────────────────────────────────────┐
│ MatrixOverviewResource                  │
│ GET /matrix?category={id}               │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ CompetencyService                       │
│ .buildMatrixViewModel(categoryId)       │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ Repositories (existing)                 │
│ - SkillRepository.findAll()             │
│ - RoleRepository.findAll()              │
│ - RoleSkillRequirementRepository        │
│   .findAll()                            │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ DTO Construction                        │
│ 1. Group roles by family                │
│ 2. Sort roles within families           │
│ 3. Create MatrixRow per skill           │
│ 4. Create MatrixCell per requirement    │
│ 5. Fill empty cells                     │
└─────────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────────┐
│ MatrixViewModel                         │
│ → Qute template (matrix-overview.html)  │
└─────────────────────────────────────────┘
```

## Service Layer Method Signature

```java
package nl.leonw.competencymatrix.service;

import nl.leonw.competencymatrix.dto.MatrixViewModel;
import java.util.Optional;

public class CompetencyService {
    // Existing methods...

    /**
     * Build complete matrix view model.
     *
     * @param categoryId Optional category filter (null = show all skills)
     * @return View model with all data needed to render matrix
     */
    public MatrixViewModel buildMatrixViewModel(Optional<Integer> categoryId) {
        // Implementation in Phase 2 (tasks.md)
    }
}
```

## Validation Rules

### MatrixViewModel
- `rows` must not be null (can be empty list if filtered to no results)
- `rolesByFamily` must not be null (can be empty map)
- `categories` must not be null (can be empty list)
- If `selectedCategoryId` is present, it must exist in `categories`

### MatrixRow
- `skill` must not be null
- `cells.size()` must equal total number of roles across all families
- Skills must be sorted alphabetically by name

### MatrixCell
- If `isEmpty` is true, `requiredLevel` must be null
- If `isEmpty` is false, `requiredLevel` must not be null
- `skillId` and `roleId` must always be present

### MatrixColumnHeader
- `role` must not be null
- `abbreviation` must not be null or empty

## Performance Considerations

### Query Optimization
- **Single query for requirements**: Load all `RoleSkillRequirement` at once, not per-skill
- **Eager loading**: Fetch related entities (Skill, Role) in initial queries to avoid N+1
- **Indexing**: Ensure indexes on `role_family`, `seniority_order` for grouping query

### Memory Efficiency
- **100 skills × 20 roles = 2,000 cells**: Approximately 200KB in memory for DTOs
- **Acceptable**: No pagination needed for this scale
- **Caching**: Consider caching `MatrixViewModel` for unfiltered view (invalidate on data changes)

### Database Query Count
- Expected: 4 queries total
  1. `SELECT * FROM skill ORDER BY name` (with optional category filter)
  2. `SELECT * FROM rolename ORDER BY role_family, seniority_order`
  3. `SELECT * FROM role_skill_requirement`
  4. `SELECT * FROM competency_category ORDER BY name`

## Example Data Structure

```java
MatrixViewModel(
    rows = [
        MatrixRow(
            skill = Skill(id=1, name="Java"),
            cells = [
                MatrixCell(skillId=1, roleId=1, requiredLevel=BASIC, isEmpty=false),
                MatrixCell(skillId=1, roleId=2, requiredLevel=DECENT, isEmpty=false),
                MatrixCell(skillId=1, roleId=3, requiredLevel=EXCELLENT, isEmpty=false),
                MatrixCell(skillId=1, roleId=4, requiredLevel=null, isEmpty=true),  // No requirement
                // ... 16 more cells
            ]
        ),
        MatrixRow(
            skill = Skill(id=2, name="Spring Boot"),
            cells = [/* 20 cells */]
        )
        // ... 98 more rows
    ],
    rolesByFamily = {
        "Developer" -> [
            MatrixColumnHeader(role=Role(id=1, name="Junior Developer"), abbrev="Jr Dev"),
            MatrixColumnHeader(role=Role(id=2, name="Medior Developer"), abbrev="Mid Dev"),
            MatrixColumnHeader(role=Role(id=3, name="Senior Developer"), abbrev="Sr Dev"),
            // ... more dev roles
        ],
        "Architect" -> [
            MatrixColumnHeader(role=Role(id=6, name="Software Architect"), abbrev="SW Arch"),
            // ... more architect roles
        ]
    },
    categories = [
        CompetencyCategory(id=1, name="Java"),
        CompetencyCategory(id=2, name="Spring"),
        // ... more categories
    ],
    selectedCategoryId = null
)
```

## Testing Requirements

### Unit Tests
- `MatrixCell.empty()` creates cell with `isEmpty=true` and `requiredLevel=null`
- `MatrixCell.getLevelCssClass()` returns correct CSS class for each level
- `MatrixColumnHeader.generateAbbreviation()` handles various name lengths
- `MatrixRow.getCellForRole()` returns correct cell or empty cell if not found

### Integration Tests
- `CompetencyService.buildMatrixViewModel()` returns correct number of rows and cells
- Filtering by category excludes skills from other categories
- Role grouping and ordering matches specification (families alphabetical, seniority within groups)
- Empty cells are created for skill-role combinations with no requirement

## Related Files

- **Domain Models**: `src/main/java/nl/leonw/competencymatrix/model/Role.java`
- **Repositories**: `src/main/java/nl/leonw/competencymatrix/repository/*Repository.java`
- **Service**: `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- **DTOs**: `src/main/java/nl/leonw/competencymatrix/dto/Matrix*.java` (NEW)
- **Migration**: `src/main/resources/db/migration/V2__add_role_grouping.sql` (NEW)
