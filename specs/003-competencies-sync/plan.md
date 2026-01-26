# Implementation Plan: Competencies Data Synchronization

**Branch**: `003-competencies-sync` | **Date**: 2026-01-26 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/003-competencies-sync/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

This feature enhances the existing DataSeeder component to support three sync modes for competencies.yaml: "none" (no sync), "merge" (incremental updates), and "replace" (full replacement). The sync mode is configured via application.properties. The implementation extends the current startup event handling to intelligently compare YAML content with database state, perform case/space-insensitive matching, and execute all changes within a transaction with comprehensive logging.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC), SnakeYAML, SLF4J
**Storage**: PostgreSQL 18.1 (via Panache JDBC and Flyway migrations)
**Testing**: JUnit 5, Quarkus Test, REST Assured, Testcontainers
**Target Platform**: Linux server (containerized with Docker)
**Project Type**: Web application (monolith with backend + server-side rendered frontend)
**Performance Goals**: Sync operations complete in <5 seconds for 100 skills + 20 roles
**Constraints**: Transaction-based with rollback on failure; <2 second UI response time (not impacted by sync)
**Scale/Scope**: ~20 categories, ~100 skills, ~10 roles; single-instance deployment

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Simplicity First
- ✅ **PASS**: Extends existing DataSeeder rather than creating new sync framework
- ✅ **PASS**: Three simple sync modes (enum-based) rather than complex configuration DSL
- ✅ **PASS**: Uses existing SnakeYAML dependency already in project
- ✅ **PASS**: Reuses existing repository layer and transaction management

### Test-First Development (NON-NEGOTIABLE)
- ✅ **COMMITTED**: TDD approach required; tests written before implementation
- ✅ **COMMITTED**: Test coverage: unit tests for each sync mode, integration tests for database operations, edge case tests for validation failures

### Monolith First Architecture
- ✅ **PASS**: Feature exists entirely within existing monolith
- ✅ **PASS**: No new services, APIs, or external dependencies introduced
- ✅ **PASS**: Uses existing Panache JDBC data layer

### Data Integrity
- ✅ **PASS**: All sync operations wrapped in @Transactional with rollback on failure
- ✅ **PASS**: YAML validation before any database modifications
- ✅ **PASS**: Referential integrity checks (skills before role requirements, roles before progressions)
- ✅ **PASS**: Case/space-insensitive matching prevents duplicate creation
- ✅ **PASS**: Clear error messages logged for all failure scenarios

### User-Centric Design
- ✅ **PASS**: Sync happens at startup, not during user requests (no UI impact)
- ⚠️ **CONSIDERATION**: Error messages must be administrator-friendly (not just developer stack traces)
- ✅ **PASS**: Comprehensive logging allows administrators to audit changes without SQL queries

**GATE STATUS**: ✅ **PASSED** - All Constitution principles satisfied with no violations

## Project Structure

### Documentation (this feature)

```text
specs/003-competencies-sync/
├── plan.md              # This file (/speckit.plan command output)
├── spec.md              # Feature specification
├── research.md          # Phase 0 output (research decisions)
├── data-model.md        # Phase 1 output (sync state model)
├── quickstart.md        # Phase 1 output (developer setup guide)
└── checklists/
    └── requirements.md  # Specification quality validation
```

### Source Code (repository root)

```text
src/main/java/nl/leonw/competencymatrix/
├── config/
│   ├── DataSeeder.java              # EXISTING - to be refactored into sync orchestrator
│   ├── SyncMode.java                # NEW - enum for none/merge/replace
│   └── CompetencySyncService.java   # NEW - core sync logic
├── model/
│   ├── CompetencyCategory.java      # EXISTING - no changes needed
│   ├── Skill.java                   # EXISTING - no changes needed
│   ├── Role.java                    # EXISTING - no changes needed
│   ├── RoleSkillRequirement.java    # EXISTING - no changes needed
│   └── RoleProgression.java         # EXISTING - no changes needed
├── repository/
│   ├── CategoryRepository.java      # EXISTING - may need findByNameIgnoreCase
│   ├── SkillRepository.java         # EXISTING - may need findByNameIgnoreCase
│   ├── RoleRepository.java          # EXISTING - may need findByNameIgnoreCase
│   ├── RoleSkillRequirementRepository.java  # EXISTING - may need deleteByRoleId
│   └── RoleProgressionRepository.java       # EXISTING - may need deleteAll
└── service/
    └── CompetencyService.java       # EXISTING - no changes needed

src/main/resources/
├── application.properties           # EXISTING - add competency.sync.mode property
└── seed/
    └── competencies.yaml            # EXISTING - no schema changes

src/test/java/nl/leonw/competencymatrix/
├── config/
│   └── CompetencySyncServiceTest.java  # NEW - unit tests for sync logic
├── integration/
│   └── CompetencySyncIntegrationTest.java  # NEW - full sync scenarios with Testcontainers
└── validation/
    └── SyncConfigurationTest.java      # NEW - test configuration validation
```

**Structure Decision**: Single project structure maintained. All sync logic lives in the config package alongside the existing DataSeeder, following the pattern of startup-time initialization. No new layers or architectural components needed - this is a surgical enhancement to existing seeding behavior.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations detected. This section is intentionally empty.*

---

## Phase 0: Research - COMPLETE ✅

**Output**: [research.md](./research.md)

**Key Decisions**:
1. **Sync Mode Configuration**: Use MicroProfile Config with @ConfigProperty injection and SyncMode enum
2. **Case/Space-Insensitive Matching**: Normalize to lowercase + collapse spaces in Java code (no DB schema changes)
3. **Transaction Management**: Single @Transactional method boundary for atomicity
4. **YAML Validation**: Validate structure immediately after parsing, before DB operations
5. **Logging Strategy**: SLF4J with individual operation logs + aggregate summary
6. **Dependency Resolution**: Process in dependency order (Categories → Skills → Roles → Requirements → Progressions)
7. **Repository Enhancements**: Add case-insensitive query methods to existing repositories
8. **Merge Mode Update Detection**: Compare all fields, update only if changed (efficiency per FR-014)
9. **Replace Mode**: Delete in reverse dependency order, then seed fresh
10. **None Mode**: Early return with WARNING log if property missing

**No new dependencies required** - all capabilities exist in current Quarkus stack.

---

## Phase 1: Design & Contracts - COMPLETE ✅

**Outputs**:
- [data-model.md](./data-model.md) - Sync configuration model, transient data structures, entity matching rules
- [quickstart.md](./quickstart.md) - Developer setup guide for working on this feature
- No contracts needed (internal feature, no external API)

**Agent Context Updated**: ✅ CLAUDE.md updated with Java 25, Quarkus 3.30.6, PostgreSQL 18.1

**Design Artifacts**:
1. **SyncMode enum**: Type-safe configuration (NONE, MERGE, REPLACE)
2. **YamlCompetencyData**: In-memory YAML representation (records)
3. **SyncResult**: Track operation outcomes for logging
4. **Entity Matching Rules**: Case/space-insensitive normalization strategy
5. **Processing Order**: Dependency-ordered entity processing
6. **Repository Query Methods**: findByNameIgnoreCase patterns
7. **Error Handling**: Validation → Referential Integrity → Database errors
8. **Observability**: INFO/WARN/ERROR logging with structured messages

**Constitution Re-Check**: ✅ **PASSED** - Design maintains all Constitution principles:
- Simplicity First: Extends existing DataSeeder, no new frameworks
- Test-First: Integration tests with Testcontainers, unit tests for sync logic
- Monolith First: Single service, single database, no distributed components
- Data Integrity: Transaction-based with validation and rollback
- User-Centric: Administrator-friendly logging and error messages

---

## Phase 2: Task Breakdown

**Next Step**: Run `/speckit.tasks` to generate [tasks.md](./tasks.md)

**Expected Task Categories**:
1. **Configuration** (SyncMode enum, property injection)
2. **YAML Parsing** (YamlCompetencyData, validation)
3. **Repository Enhancements** (case-insensitive queries, deleteAll methods)
4. **Sync Service** (merge/replace/none logic)
5. **DataSeeder Refactoring** (integrate sync modes)
6. **Testing** (unit tests, integration tests, edge cases)
7. **Documentation** (README updates, configuration examples)

All tasks will follow **TDD approach** per Constitution: write tests first, then implementation.

---

## Summary

**Implementation Plan Complete** ✅

This plan provides a comprehensive technical foundation for implementing the competencies data synchronization feature:

- **Research decisions** documented with rationale and alternatives
- **Data model** defined with entity matching rules and processing order
- **Developer quickstart** guide for setup and testing workflows
- **Constitution compliance** verified at all stages
- **No new dependencies** required - leverages existing Quarkus stack
- **Performance target** achievable: <5 seconds for 100 skills + 20 roles

**Ready for task generation**: `/speckit.tasks`

