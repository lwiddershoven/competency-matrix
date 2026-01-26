# Implementation Plan: Platform Modernization to Quarkus

**Branch**: `002-platform-modernization` | **Date**: 2026-01-21 | **Spec**: [spec.md](spec.md)
**Input**: Migration to Quarkus 3.30.6 including replacement of Liquibase if appropriate and property files. No reactive APIs.

## Summary

Migrate the Career Competency Matrix application from Spring Boot 4 to Quarkus 3.30.6 while preserving all existing functionality, database schema, and user experience. The migration will replace Spring-specific dependencies with Quarkus equivalents, convert configuration from Spring Boot's application.yaml to Quarkus application.properties, and evaluate database migration tool alternatives to Liquibase. The application will continue to use blocking/traditional (non-reactive) APIs to maintain simplicity and align with current architecture patterns.

## Technical Context

**Language/Version**: Java 25
**Current Framework**: Spring Boot 4.0.1 (to be migrated)
**Target Framework**: Quarkus 3.30.6 (blocking/traditional, non-reactive)
**Primary Dependencies**:
- Current: Spring Web, Spring Data JDBC, Thymeleaf, Liquibase, htmx-spring-boot-thymeleaf
- Target: Quarkus RESTEasy Classic/Reactive (blocking), Quarkus JDBC, Qute templating, NEEDS CLARIFICATION: database migration tool, NEEDS CLARIFICATION: htmx integration approach

**Storage**: PostgreSQL 18.1 (no changes to schema or data)
**Testing**:
- Current: JUnit 5, Spring Boot Test, Testcontainers, Playwright
- Target: JUnit 5, Quarkus Test, Testcontainers, Playwright

**Target Platform**: Linux server (containerized deployment)
**Project Type**: Web application (monolith - server-rendered HTML with htmx)
**Performance Goals**: <2 second page load times, support current concurrent user capacity
**Constraints**:
- Zero database schema changes
- Zero data loss or migration
- Preserve all URL patterns
- Maintain monolith deployment model
- No reactive programming (use blocking/traditional APIs)

**Scale/Scope**:
- ~27 Java source files (controllers, services, repositories, models, config)
- 6 entities (Role, Skill, CompetencyCategory, ProficiencyLevel, RoleSkillRequirement, RoleProgression)
- 13 test files (unit, integration, e2e)
- 5 HTML templates
- 1 Liquibase changelog

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### I. Simplicity First ✅

**Compliance**: PASS

- Migration maintains current simplicity: monolithic web application with server-rendered HTML
- No new architectural patterns introduced beyond framework-specific equivalents
- Avoiding reactive programming per user requirement maintains simpler mental model
- Standard library approach: Quarkus provides built-in equivalents for current Spring features

**Justification**: The migration replaces one mainstream framework (Spring Boot) with another (Quarkus) while preserving the same architectural patterns. No additional abstraction layers or complexity being introduced.

### II. Test-First Development (NON-NEGOTIABLE) ✅

**Compliance**: PASS

- Existing test suite (M test cases) must continue to pass per FR-007 and SC-002
- Migration approach: tests will be adapted to Quarkus testing APIs but maintain identical assertions
- Test-first principle applies: verify current test coverage, migrate tests before implementation, ensure green tests throughout

**Strategy**:
1. Document current test coverage baseline
2. Migrate test infrastructure (Quarkus Test replaces Spring Boot Test)
3. Update test assertions for Quarkus-specific APIs
4. Maintain Red-Green-Refactor: tests must pass after each migration step

### III. Monolith First Architecture ✅

**Compliance**: PASS

- Application remains single deployable unit (JAR/native binary)
- No microservices, message queues, or distributed systems introduced
- Database access continues through single data layer (repositories)
- Quarkus supports same monolith deployment model

**Verification**: FR-010 explicitly requires maintaining monolith deployment per constitution

### IV. Data Integrity ✅

**Compliance**: PASS

- FR-003: Database schema structure unchanged (zero DDL modifications)
- FR-004: All existing data preserved without modification
- FR-006: Continue using existing database instance
- FR-013: Automated data integrity validation before/after migration

**Migration Strategy**:
- Database migration tool (Liquibase replacement) must support existing changelog format or require conversion
- All existing Liquibase changesets must be preserved/converted
- Referential integrity constraints remain unchanged
- No data transformation or migration required

### V. User-Centric Design ✅

**Compliance**: PASS

- FR-001: All current features maintained (role browsing, comparison, progressions)
- FR-002: All URLs and navigation patterns preserved
- NFR-001: Zero breaking changes to UI/UX
- SC-003: Page load times remain <2 seconds

**Verification**:
- VC-002: Pixel-perfect match for page rendering
- VC-004: End-to-end tests validate user workflows
- VC-005: Manual UAT confirms identical user experience

### Quality Standards ✅

**Compliance**: PASS

- Code quality maintained: Quarkus code will follow same linting/formatting standards
- Documentation: Architecture decision recorded in this plan
- Performance: NFR-002 requires no query performance regression

### Gate Evaluation

**GATE STATUS**: ✅ PASS - Proceed to Phase 0

All constitution principles are maintained. No violations requiring justification.

## Project Structure

### Documentation (this feature)

```text
specs/002-platform-modernization/
├── plan.md              # This file
├── research.md          # Phase 0: Technology evaluation
├── data-model.md        # Phase 1: Entity mapping (no schema changes)
├── quickstart.md        # Phase 1: Migration runbook
├── contracts/           # Phase 1: API contracts (if any changes)
└── tasks.md             # Phase 2: Implementation tasks (/speckit.tasks)
```

### Source Code (repository root)

Current structure (Spring Boot):

```text
src/
├── main/
│   ├── java/nl/leonw/competencymatrix/
│   │   ├── CompetencyMatrixApplication.java
│   │   ├── config/          # DataSeeder
│   │   ├── controller/      # HomeController, RoleController, CompareController
│   │   ├── model/           # Role, Skill, Category, ProficiencyLevel, etc.
│   │   ├── repository/      # Spring Data JDBC repositories
│   │   └── service/         # CompetencyService
│   └── resources/
│       ├── application.yaml
│       ├── db/changelog/    # Liquibase migrations
│       ├── seed/            # competencies.yaml
│       ├── templates/       # Thymeleaf templates
│       └── static/css/      # Custom CSS
└── test/
    ├── java/nl/leonw/competencymatrix/
    │   ├── controller/      # Controller tests
    │   ├── repository/      # Repository tests
    │   ├── service/         # Service tests
    │   ├── e2e/             # Playwright E2E tests
    │   ├── TestCompetencyMatrixApplication.java
    │   └── TestcontainersConfiguration.java
    └── resources/
```

Target structure (Quarkus):

```text
src/
├── main/
│   ├── java/nl/leonw/competencymatrix/
│   │   ├── config/          # Startup/lifecycle beans (DataSeeder)
│   │   ├── resource/        # JAX-RS resources (renamed from controller)
│   │   ├── model/           # Entities (no changes)
│   │   ├── repository/      # Quarkus Panache or plain JDBC repositories
│   │   └── service/         # Business logic (minimal changes)
│   └── resources/
│       ├── application.properties  # Converted from application.yaml
│       ├── db/migration/    # Flyway migrations (if migrating from Liquibase)
│       ├── seed/            # competencies.yaml (no changes)
│       ├── templates/       # Qute templates (converted from Thymeleaf)
│       └── META-INF/resources/css/  # Static resources
└── test/
    ├── java/nl/leonw/competencymatrix/
    │   ├── resource/        # JAX-RS resource tests (adapted from controller tests)
    │   ├── repository/      # Repository tests
    │   ├── service/         # Service tests
    │   ├── e2e/             # Playwright E2E tests (minimal changes)
    │   └── TestcontainersConfiguration.java (Quarkus DevServices)
    └── resources/
        ├── application.properties  # Test configuration
```

**Structure Decision**: The directory structure follows Quarkus conventions while maintaining similar organization to Spring Boot. Key changes:
- `controller/` → `resource/` (JAX-RS terminology)
- `application.yaml` → `application.properties` (Quarkus standard)
- Thymeleaf templates → Qute templates (Quarkus templating engine)
- Liquibase changelog → Database migration tool (to be determined in research phase)

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

No violations detected. Table not required.

## Phase 0: Research & Technology Decisions

*Status: PENDING - To be generated in research.md*

### Research Questions

The following unknowns from Technical Context require resolution:

1. **Database Migration Tool**
   - Question: Should we replace Liquibase with Flyway, or keep Liquibase with Quarkus?
   - Context: Quarkus supports both Flyway (first-class) and Liquibase (community extension)
   - Research needed: Compare Flyway vs Liquibase Quarkus extension, migration path, feature parity

2. **Templating Engine**
   - Question: Use Qute (Quarkus native) or Quarkus Thymeleaf extension?
   - Context: Current app uses Thymeleaf templates
   - Research needed: Qute vs Thymeleaf performance, migration effort, feature compatibility

3. **htmx Integration**
   - Question: How to integrate htmx with Quarkus (no spring-boot-htmx-thymeleaf equivalent)?
   - Context: Current app uses io.github.wimdeblauwe:htmx-spring-boot-thymeleaf
   - Research needed: Direct htmx usage, Qute integration patterns, Spring htmx library compatibility

4. **RESTEasy Classic vs Reactive**
   - Question: Use RESTEasy Classic (blocking) or RESTEasy Reactive in blocking mode?
   - Context: User specified no reactive APIs, but RESTEasy Reactive can run in blocking mode
   - Research needed: Performance comparison, API compatibility, blocking guarantees

5. **Spring Data JDBC Replacement**
   - Question: Use Quarkus Panache for Repositories, Hibernate with Active Record, or plain JDBC?
   - Context: Current app uses Spring Data JDBC (not JPA/Hibernate)
   - Research needed: Closest Quarkus equivalent to Spring Data JDBC patterns, migration complexity

6. **Observability/Metrics**
   - Question: Quarkus Micrometer vs SmallRye Metrics for Prometheus?
   - Context: Current app uses Micrometer with Prometheus registry
   - Research needed: Feature parity, endpoint compatibility (/actuator/prometheus preservation)

7. **Test Infrastructure**
   - Question: Quarkus Test framework vs QuarkusTestResource for Testcontainers?
   - Context: Current tests use Spring Boot Test slices (@WebMvcTest, @DataJdbcTest, @JdbcTest)
   - Research needed: Quarkus DevServices, Testcontainers integration, test slice equivalents

### Research Tasks

Phase 0 will generate research.md with decisions on:

- **Database migrations**: Flyway vs Liquibase, changelog conversion strategy
- **Templating**: Qute vs Thymeleaf Quarkus extension, template migration approach
- **htmx integration**: Direct integration pattern, request/response handling
- **REST framework**: RESTEasy Classic vs Reactive (blocking mode), API design
- **Data access**: Repository pattern implementation (Panache vs plain JDBC)
- **Configuration**: application.yaml → application.properties conversion rules
- **Observability**: Metrics/health endpoint preservation strategy
- **Testing**: Test framework migration, Testcontainers integration, assertion mapping

## Phase 1: Design & Contracts

*Status: PENDING - To be generated after Phase 0*

### Artifacts to Generate

1. **data-model.md**: Entity mapping documentation
   - Current entities unchanged (6 entities from spec)
   - Document Quarkus annotations (if using Panache/Hibernate)
   - Validation annotations mapping (Jakarta vs Spring)

2. **contracts/**: API contracts
   - HTTP endpoints (if URL patterns change from Spring MVC to JAX-RS)
   - Template contracts (Thymeleaf → Qute syntax mapping)
   - Database contracts (schema verification, no changes expected)

3. **quickstart.md**: Migration runbook
   - Pre-migration checklist (backup, baseline metrics)
   - Step-by-step migration procedure
   - Validation procedures (test execution, data integrity checks)
   - Rollback procedure

## Phase 2: Task Generation

*Status: NOT STARTED - Run `/speckit.tasks` after Phase 1 complete*

Task generation will be performed by the `/speckit.tasks` command based on this plan and Phase 0/1 artifacts.

## Risks & Mitigations

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Template migration breaks UI | High | Medium | Pixel-perfect comparison tests, gradual template migration |
| htmx integration incompatibility | High | Low | Research phase validates integration pattern, fallback to vanilla JS |
| Test suite migration incomplete | High | Medium | Document test coverage baseline, verify 100% coverage post-migration |
| Performance regression | Medium | Medium | Baseline performance tests, comparative benchmarking |
| Database migration tool issues | Medium | Low | Validate migration tool in Phase 0, test on staging database |
| URL pattern changes | High | Low | JAX-RS path mapping preserves Spring MVC routes |

## Success Criteria Alignment

This plan addresses all Success Criteria from spec.md:

- **SC-001**: All features functional → Comprehensive test suite migration
- **SC-002**: Critical workflows error-free → End-to-end test preservation
- **SC-003**: <2s page loads → Performance benchmarking in migration runbook
- **SC-004**: Data complete/accurate → FR-003/004 compliance, zero schema changes
- **SC-005**: Zero data loss → Data integrity validation pre/post migration
- **SC-006**: Concurrent user capacity → Performance testing, load benchmarks
- **SC-007**: 99.9% availability → Rollback procedure, staged deployment
- **SC-008**: Bookmarks work → URL preservation validation
- **SC-009**: 5min rollback → Documented rollback procedure in quickstart.md
- **SC-010**: Resource consumption → Memory/CPU monitoring, NFR-004 compliance

## Next Steps

1. ✅ **Phase 0 Complete**: Research technology decisions → Generate `research.md`
2. **Phase 1 Complete**: Design artifacts → Generate `data-model.md`, `contracts/`, `quickstart.md`
3. **Update Agent Context**: Run `.specify/scripts/bash/update-agent-context.sh claude`
4. **Re-evaluate Constitution Check**: Verify no violations introduced by design decisions
5. **Report**: Branch, plan path, generated artifacts
