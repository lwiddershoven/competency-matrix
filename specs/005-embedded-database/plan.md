# Implementation Plan: In-Memory Database Migration

**Branch**: `005-embedded-database` | **Date**: 2026-01-27 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/005-embedded-database/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

Replace external PostgreSQL database with H2 in-memory database to eliminate external dependencies while preserving the existing YAML-based data management workflow. The `competencies.yaml` file remains the single source of truth, with data loaded into H2 on each application startup via the existing CompetencySyncService. All repository methods, Flyway migrations, and sync modes (REPLACE, MERGE, NONE) continue working without modification.

**Key Benefits**: Zero-setup development experience, simplified deployment (no database provisioning), faster test execution (no test containers), git-based data versioning through existing YAML workflow.

## Technical Context

**Language/Version**: Java 25
**Primary Dependencies**: Quarkus 3.30.6 (JAX-RS, Qute, Panache JDBC, Flyway, SmallRye Health, Micrometer), SnakeYAML 2.3, SLF4J
**Storage**: H2 in-memory database (replacing PostgreSQL 18.1), JDBC-based repositories with PreparedStatements
**Testing**: JUnit 5, Quarkus Test, REST Assured, Testcontainers (to be removed), Playwright
**Target Platform**: JVM-based server, Clever Cloud deployment
**Project Type**: Web application (backend with server-side rendered UI via Qute + htmx)
**Performance Goals**: <5 second application startup, <2 second page load, <30 second full test suite execution
**Constraints**: <100MB memory overhead for in-memory database, 100% data accuracy compared to YAML source, maintain existing SQL compatibility
**Scale/Scope**: ~40 skills, ~10 roles, ~6 categories, single-user development tool (no concurrent write operations)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Simplicity First ✅ PASS

**Evaluation**: This feature actively reduces complexity by eliminating external PostgreSQL dependency. Developers no longer need to install, configure, or manage a separate database service. The solution uses H2 in-memory database, a mature and widely-adopted standard library alternative.

**Justification**:
- Removes external service dependency (PostgreSQL)
- Zero additional abstractions (uses existing JDBC interfaces)
- No new frameworks or libraries (H2 is embedded, minimal footprint)
- Simpler than current architecture (fewer moving parts)

### II. Test-First Development (NON-NEGOTIABLE) ✅ PASS

**Commitment**: Implementation will follow TDD workflow:
1. Write test verifying H2 database connection works
2. Write test verifying Flyway migrations apply successfully to H2
3. Write test verifying CompetencySyncService loads YAML data into H2
4. Update configuration to use H2
5. Run full test suite to verify all existing tests pass
6. Manual testing of UI and sync modes

**Verification**: All existing tests must pass without modification (FR-015, SC-003).

### III. Monolith First Architecture ✅ PASS

**Evaluation**: No architectural changes. Application remains a single deployable Quarkus JAR with embedded database. No microservices, message queues, or distributed systems introduced.

**Structure**: Single backend service, single frontend (server-side Qute templates), single database (now embedded instead of external).

### IV. Data Integrity ✅ PASS

**Evaluation**: Data integrity is maintained and improved:
- YAML remains single source of truth (FR-010)
- Referential integrity constraints preserved in H2 (FR-008)
- Flyway migrations ensure schema consistency (FR-003)
- CompetencySyncService validation unchanged (FR-004, FR-005)
- Data reload from YAML on each restart ensures consistency

**Improvement**: No risk of database state diverging from YAML source between restarts.

### V. User-Centric Design ✅ PASS

**Evaluation**: Feature directly improves user experience:
- New developers: Clone → Run in under 2 minutes (SC-001)
- No 3-click rule impact (UI unchanged)
- Response times maintained or improved (SC-007: <2 seconds)
- Clear feedback via startup logging (FR-012)
- Zero breaking changes to existing workflows (FR-015)

**User Benefit**: Eliminates "database setup" barrier for new contributors.

### Constitution Compliance Summary

| Principle | Status | Notes |
|-----------|--------|-------|
| Simplicity First | ✅ PASS | Reduces complexity by removing external dependency |
| Test-First Development | ✅ PASS | TDD workflow committed, existing tests verify compatibility |
| Monolith First | ✅ PASS | No architectural changes, remains single deployable unit |
| Data Integrity | ✅ PASS | YAML source of truth preserved, constraints maintained |
| User-Centric Design | ✅ PASS | Improves developer onboarding experience significantly |

**GATE STATUS: ✅ APPROVED** - All constitutional principles satisfied. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/main/
├── java/nl/leonw/competencymatrix/
│   ├── config/
│   │   ├── CompetencySyncService.java  # UNCHANGED - database-agnostic YAML loader
│   │   └── DataSeeder.java              # UNCHANGED - calls sync on startup
│   ├── model/
│   │   ├── CompetencyCategory.java      # UNCHANGED - Java Record
│   │   ├── Role.java                    # UNCHANGED - Java Record
│   │   ├── Skill.java                   # UNCHANGED - Java Record
│   │   ├── RoleSkillRequirement.java    # UNCHANGED - Java Record
│   │   ├── RoleProgression.java         # UNCHANGED - Java Record
│   │   └── ProficiencyLevel.java        # UNCHANGED - Enum
│   ├── repository/
│   │   ├── SkillRepository.java         # UNCHANGED - JDBC PreparedStatements
│   │   ├── RoleRepository.java          # UNCHANGED - JDBC PreparedStatements
│   │   ├── CategoryRepository.java      # UNCHANGED - JDBC PreparedStatements
│   │   ├── RoleSkillRequirementRepository.java  # UNCHANGED
│   │   └── RoleProgressionRepository.java       # UNCHANGED
│   └── web/
│       └── CompetencyResource.java      # UNCHANGED - JAX-RS endpoints
└── resources/
    ├── application.properties           # MODIFIED - datasource config (H2)
    ├── db/migration/
    │   ├── V1__initial_schema.sql       # UNCHANGED - standard SQL DDL
    │   └── V2__add_role_grouping.sql    # UNCHANGED - standard SQL DDL
    └── seed/
        └── competencies.yaml            # UNCHANGED - source of truth

src/test/
├── java/nl/leonw/competencymatrix/
│   ├── repository/
│   │   └── *RepositoryTest.java         # UNCHANGED - will use H2 automatically
│   └── web/
│       └── CompetencyResourceTest.java  # UNCHANGED - REST Assured tests
└── resources/
    └── application.properties           # MODIFIED - remove test container config

pom.xml                                   # MODIFIED - swap postgresql for h2 dependency
clevercloud/maven.json                    # UNCHANGED - build/deploy config
```

**Structure Decision**: Monolithic web application with server-side rendering (Qute templates). This is an **in-place configuration change** - almost all source code remains unchanged. Only `pom.xml` and `application.properties` files are modified. The existing JDBC-based architecture was designed to be database-agnostic, so swapping PostgreSQL for H2 requires zero code changes.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

**Status**: No complexity violations. This feature actively reduces complexity by eliminating external database dependency.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |
