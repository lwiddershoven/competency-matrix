# Implementation Plan: Multi-File Competencies Data

**Branch**: `006-competencies-split` | **Date**: 2026-01-28 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-competencies-split/spec.md`

## Summary

Split the 678-line `competencies.yaml` file into multiple smaller files (one per category, one per role) by modifying only the YAML loading mechanism in `CompetencySyncService`. The approach is minimal: change the `loadYamlData()` method to read and merge multiple files instead of one, while keeping all other code (parsing, validation, database sync) unchanged.

**Key Insight**: The existing code already has clean separation - `loadYamlData()` returns a `YamlCompetencyData` object. We only need to change HOW that object is built (from multiple files instead of one), not WHAT it contains.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC, Flyway), SnakeYAML 2.3, SLF4J
**Storage**: H2 embedded database (in-memory), JDBC-based repositories with PreparedStatements
**Testing**: JUnit 5, Quarkus Test, REST Assured, Testcontainers (PostgreSQL for integration tests), Playwright
**Target Platform**: JVM server application
**Project Type**: Single monolith (backend + server-side rendered frontend)
**Performance Goals**: Application startup within 2 seconds (including YAML loading and database sync)
**Constraints**: Zero data loss during migration, backward compatibility with existing data model
**Scale/Scope**: 15 top-level YAML entries (6 categories + 9 roles), ~678 lines total, ~47 Java files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Simplicity First ✅ PASS

**Assessment**: This plan follows the simplest approach possible for the requirement.

- Reuses 100% of existing parsing logic (`parseCategories()`, `parseRoles()`, `parseProgressions()`)
- Reuses 100% of existing validation logic (`validateYaml()`)
- Changes only ONE method: `loadYamlData()` - to read multiple files and merge results
- No new abstractions, patterns, or frameworks introduced
- File discovery uses simple directory listing (Java NIO `Files.list()`)
- No configuration changes needed (files discovered automatically)

**Alternatives Considered & Rejected**:
- External config file listing all files → unnecessary complexity
- Convention-based subdirectories (categories/, roles/) → violates YAGNI
- Factory pattern for loaders → over-engineering for simple file I/O

### II. Test-First Development (NON-NEGOTIABLE) ✅ WILL COMPLY

**Commitment**: All tests will be written BEFORE implementation code.

**Test Strategy**:
1. Unit tests for new file discovery logic (before implementing file listing)
2. Unit tests for merging multiple YAML results (before implementing merge logic)
3. Integration test for multi-file loading (before changing loadYamlData())
4. Integration test for edge cases (empty files, duplicates, missing files)

**Existing Test Coverage to Preserve**:
- `CompetencySyncServiceTest` already tests single-file parsing and validation
- Will extend these tests to cover multi-file scenarios
- All existing tests must continue passing (backward compatibility)

### III. Monolith First Architecture ✅ PASS

**Assessment**: No architectural changes. Feature is entirely within existing monolith.

- All changes in `CompetencySyncService.java` (existing class)
- No new services, modules, or deployment units
- Single deployable JAR remains unchanged
- Database access through existing repositories unchanged

### IV. Data Integrity ✅ PASS

**Assessment**: Maintains existing data integrity guarantees, adds file-level validation.

**Preservation**:
- Existing `validateYaml()` method ensures schema correctness (reused as-is)
- Database sync uses existing transactional boundaries (`@Transactional`)
- Fail-fast on startup if any file is malformed (existing behavior preserved)

**Enhancement**:
- Per-file error reporting (filename + line number) improves debugging
- Duplicate category detection across files prevents silent data corruption

### V. User-Centric Design ✅ PASS

**Assessment**: Directly addresses user pain point (678-line file maintainability).

- Business editors can edit category-specific files (~50-100 lines each)
- File names match category names (programming.yaml, devops-infrastructure.yaml)
- Error messages identify specific files on validation failure
- No UI changes needed (editors use text editors as before)
- Application startup remains under 2 seconds (SC-002)

### Gate Status: ✅ ALL GATES PASS - Proceed to Phase 0

---

## Post-Design Constitution Re-Check

*Re-evaluated after Phase 0 (research.md) and Phase 1 (data-model.md, quickstart.md) completion*

### I. Simplicity First ✅ PASS (Confirmed)

**Design Validation**:
- Research confirmed: `Files.list()` + `List.addAll()` = ~65 lines of new code
- No new classes, no new dependencies, no new patterns
- File discovery: 10 lines, Merging: 20 lines, Validation: 30 lines
- All estimates within original "~50 lines" target (acceptable variance)

**Complexity Justification**: NONE NEEDED - remains simplest possible solution.

### II. Test-First Development ✅ WILL COMPLY (Confirmed)

**Design-Informed Test Plan**:
1. Unit test for `discoverYamlFiles()` - verify file discovery logic
2. Unit test for merging lists with `addAll()` - verify order preservation
3. Unit test for duplicate detection - verify Map-based tracking
4. Integration test for multi-file loading - verify end-to-end flow
5. Integration test for edge cases - empty files, missing dirs, duplicates

**TDD Workflow**: All tests written in `/speckit.tasks` phase before implementation.

### III. Monolith First Architecture ✅ PASS (Confirmed)

**Design Validation**:
- Zero new services or modules
- All changes in `CompetencySyncService.java` (existing class)
- Project structure unchanged (still single-project monolith)

### IV. Data Integrity ✅ PASS (Enhanced)

**Design Validation**:
- Duplicate detection prevents data corruption (research.md: HashMap tracking)
- Fail-fast on schema mismatch (existing `validateYaml()` reused)
- Per-file error reporting improves debugging (filename + line number)
- Migration verification: split files must sum to original data

**Enhancement Over Original**: Duplicate detection across files is NEW integrity guarantee.

### V. User-Centric Design ✅ PASS (Confirmed)

**Design Validation**:
- quickstart.md provides non-technical editing guide (user-focused)
- File naming convention matches display names (intuitive mapping)
- Error messages identify specific files (actionable feedback)
- Performance budget met: ~50ms overhead for 15 files (well under 2-second budget)

### Final Gate Status: ✅ ALL GATES PASS - Ready for Phase 2 (`/speckit.tasks`)

## Project Structure

### Documentation (this feature)

```text
specs/006-competencies-split/
├── plan.md              # This file
├── research.md          # Phase 0: File discovery & merging patterns
├── data-model.md        # Phase 1: Split file structure & schema
├── quickstart.md        # Phase 1: How to edit split files
├── contracts/           # N/A - no API changes
└── tasks.md             # Phase 2: TDD implementation tasks
```

### Source Code (repository root)

```text
src/main/java/nl/leonw/competencymatrix/
├── config/
│   ├── CompetencySyncService.java       # MODIFY: loadYamlData() method
│   ├── YamlCompetencyData.java          # UNCHANGED: data model
│   └── DataSeeder.java                  # UNCHANGED: startup trigger
├── model/                                # UNCHANGED: database entities
├── repository/                           # UNCHANGED: data access
└── resource/                             # UNCHANGED: REST endpoints

src/main/resources/
├── seed/
│   ├── categories/                       # NEW: category files
│   │   ├── programming.yaml
│   │   ├── software-design.yaml
│   │   ├── devops-infrastructure.yaml
│   │   ├── quality-testing.yaml
│   │   ├── soft-skills.yaml
│   │   └── architecture-frameworks.yaml
│   ├── roles/                            # NEW: role files
│   │   ├── junior-developer.yaml
│   │   ├── medior-developer.yaml
│   │   ├── senior-developer.yaml
│   │   ├── specialist-developer.yaml
│   │   ├── lead-developer.yaml
│   │   ├── lead-developer-software-architect.yaml
│   │   ├── software-architect.yaml
│   │   ├── solution-architect.yaml
│   │   └── devops-engineer.yaml
│   └── competencies.yaml                 # KEEP: for backward compat testing
└── application.properties                # UNCHANGED

src/test/java/nl/leonw/competencymatrix/
└── config/
    └── CompetencySyncServiceTest.java    # EXTEND: add multi-file tests
```

**Structure Decision**: Keep existing single-project structure. All changes confined to:
1. `CompetencySyncService.loadYamlData()` - change file reading logic
2. `src/main/resources/seed/` - add categories/ and roles/ subdirectories
3. `CompetencySyncServiceTest` - extend with multi-file test cases

No new Java classes needed. No new dependencies needed.

## Complexity Tracking

> **No violations - this section intentionally empty.**

The plan adheres to all Constitution principles without requiring exceptions.

---

# Phase 0: Research & Investigation

## Research Tasks

### R1: File Discovery Pattern (Java NIO)

**Question**: What's the simplest way to discover all .yaml files in categories/ and roles/ directories?

**Investigation**:
- Review Java NIO `Files.list()` vs `Files.walk()` for directory listing
- Determine if recursive search is needed (answer: no - single directory depth)
- Evaluate error handling for missing directories (fail vs. return empty list)

**Deliverable**: Code snippet showing file discovery with proper resource management (try-with-resources)

### R2: YAML Merging Strategy

**Question**: How to merge multiple parsed YAML results into single `YamlCompetencyData` object?

**Current State**:
- `parseCategories()` returns `List<CategoryData>`
- `parseRoles()` returns `List<RoleData>`
- Single file: one call per method, single result

**New State**:
- Multiple files: multiple calls per method, need to merge lists
- Example: programming.yaml → CategoryData("Programming", [...skills]), software-design.yaml → CategoryData("Software Design", [...skills])
- Merge: combine both CategoryData objects into single list

**Investigation**:
- Simple `List.addAll()` sufficient? (answer: likely yes)
- Order preservation needed? (answer: yes - display order matters)
- Memory efficiency for 15 files? (answer: non-issue, small data set)

**Deliverable**: Pseudocode for merging pattern

### R3: Edge Case Handling

**Question**: How to handle the edge cases from spec.md (lines 60-65)?

**Cases to Research**:
1. Empty file → Current `parseYaml()` behavior with empty map
2. Missing directory → Java NIO exception vs. return empty list
3. Duplicate category names → Detection logic (Map to track seen names?)
4. Invalid YAML → SnakeYAML exception handling (already exists, verify propagation)
5. Schema mismatch → Existing `validateYaml()` catches this

**Deliverable**: Decision matrix for each edge case (fail-fast vs. log-warning vs. ignore)

## Research Output Location

All research findings will be documented in `specs/006-competencies-split/research.md`.

---

# Phase 1: Design Artifacts

## Data Model (data-model.md)

**Purpose**: Document the split file structure and schema.

**Content**:
1. File organization (categories/ and roles/ directories)
2. File naming convention (category name → filename transformation)
3. YAML schema for category files (identical to current categories section)
4. YAML schema for role files (identical to current roles section)
5. Example files with annotations

**Key Decision**: Each file contains ONLY its entity (one category OR one role), not a wrapper object. Simplest possible structure.

Example category file (programming.yaml):
```yaml
name: "Programming"
displayOrder: 1
skills:
  - name: "Java"
    levels:
      basic: "..."
      decent: "..."
      good: "..."
      excellent: "..."
```

Example role file (junior-developer.yaml):
```yaml
name: "Junior Developer"
description: "..."
roleFamily: "Development"
seniorityOrder: 1
requirements:
  - skillName: "Java"
    categoryName: "Programming"
    level: "basic"
```

## Contracts

**Decision**: No API contracts needed. This is an internal data loading change with zero impact on REST endpoints or external interfaces.

`contracts/` directory will remain empty with a README explaining why.

## Quickstart Guide (quickstart.md)

**Purpose**: Guide business editors on editing split files.

**Content**:
1. Where to find files (src/main/resources/seed/categories/ and roles/)
2. Which file to edit for which change (e.g., Java skill → programming.yaml)
3. YAML syntax reminders (indentation, quotes, lists)
4. How to test changes (restart application, check logs for errors)
5. Common mistakes (duplicate names, wrong indentation, missing levels)

**Audience**: Non-technical business editors, not developers.

---

# Phase 2: Implementation Tasks (Placeholder)

**Note**: Detailed TDD tasks will be generated by `/speckit.tasks` command after this plan is approved.

**High-Level Task Breakdown** (for planning purposes only):

1. **Create split YAML files** (one-time migration)
   - Script to split competencies.yaml into category and role files
   - Verify split files parse correctly with existing logic

2. **Modify CompetencySyncService.loadYamlData()** (TDD cycle)
   - Test: Discover category files in categories/ directory
   - Code: Implement file discovery with Files.list()
   - Test: Discover role files in roles/ directory
   - Code: Reuse file discovery logic
   - Test: Parse and merge multiple category files
   - Code: Loop + parseCategories() + merge
   - Test: Parse and merge multiple role files
   - Code: Loop + parseRoles() + merge
   - Test: Handle empty directory (no files found)
   - Code: Return empty lists for empty directories
   - Test: Detect duplicate category names across files
   - Code: Track seen names, error on duplicates
   - Test: Report filename on YAML parse error
   - Code: Wrap parse exceptions with filename context

3. **Integration testing**
   - Test: Load all split files, verify data matches original
   - Test: Modify one category file, verify only that category changes
   - Test: Edge cases (empty file, malformed YAML, duplicate names)

4. **Documentation update**
   - Update README with new file structure
   - Add migration guide for future edits

**Estimated Complexity**: LOW
- ~50 lines of new code in loadYamlData()
- ~100 lines of new test code
- 15 new YAML files (split from existing)
- Zero changes to 46 other Java files

---

# Risks & Mitigation Strategies

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| File discovery returns files in wrong order | Medium | Low | Sort files alphabetically, rely on displayOrder field in YAML for actual ordering |
| Editors forget to update both category and role files | Low | Medium | Quickstart guide with cross-references, validation ensures consistency |
| Performance degradation from N files | Very Low | Low | Success criterion SC-002 (2sec startup), 15 files is trivial I/O |
| Migration script loses data | Low | High | Verify split files sum to original, keep competencies.yaml in git history |
| IDE/editor doesn't recognize YAML in subdirs | Very Low | Low | Standard .yaml extension, editors auto-detect |

---

# Appendix: Key File References

- `CompetencySyncService.java:300-310` - loadYamlData() method (MODIFY)
- `CompetencySyncService.java:102-119` - parseYaml() method (REUSE)
- `CompetencySyncService.java:609-685` - parseCategories/Roles/Progressions (REUSE)
- `CompetencySyncService.java:127-193` - validateYaml() method (REUSE)
- `YamlCompetencyData.java` - Data model records (UNCHANGED)
- `DataSeeder.java:18-20` - Startup trigger (UNCHANGED)
- `src/main/resources/seed/competencies.yaml` - Source to split (KEEP for tests)
