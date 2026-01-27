# Feature Specification: In-Memory Database Migration

**Feature Branch**: `005-embedded-database`
**Created**: 2026-01-27
**Status**: Draft
**Input**: User description: "For maintenance purposes we don't want a separate database anymore but an embedded database so that data is properly managed through git. Just to clarify the scope: The current structure with competencies.yaml is excellent - we just want to read it into an internal in-memory SQL database and not an external postgres database)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Zero-Setup Development (Priority: P1)

New developers can clone the repository and start the application immediately without installing, configuring, or running an external PostgreSQL database service. The application uses an in-memory database that is automatically initialized from the existing `competencies.yaml` file on startup.

**Why this priority**: This is the core goal - eliminating the external database dependency. Without this, the feature has no value.

**Independent Test**: Clone repository, run `mvn quarkus:dev` without starting PostgreSQL, verify application starts successfully and displays competency matrix with all data from YAML pre-populated. No database installation, connection configuration, or manual data setup required.

**Acceptance Scenarios**:

1. **Given** a fresh clone of the repository, **When** developer runs `mvn quarkus:dev` without PostgreSQL running, **Then** application starts successfully and serves competency matrix UI with all skills, roles, and requirements loaded from YAML
2. **Given** no database credentials configured, **When** application initializes, **Then** in-memory database is created automatically and schema migrations are applied
3. **Given** application has started successfully, **When** viewing the competency matrix, **Then** all data matches the content defined in `competencies.yaml`

---

### User Story 2 - Simplified Deployment (Priority: P2)

Deployment to hosting platforms requires zero external database provisioning or configuration. The application JAR contains everything needed to run, with data automatically loaded from the bundled YAML file.

**Why this priority**: Reduces operational complexity and deployment dependencies. Builds on P1 by extending it to production environments.

**Independent Test**: Build production JAR with `mvn package`, deploy to Clever Cloud with only basic application configuration (no database environment variables), verify application starts and serves competency data correctly.

**Acceptance Scenarios**:

1. **Given** production JAR is built and deployed, **When** application starts on hosting platform, **Then** no external database connection configuration is required
2. **Given** application is deployed to Clever Cloud, **When** no CC_POSTGRESQL_* environment variables are set, **Then** application uses in-memory database automatically
3. **Given** application is running in production, **When** serving competency matrix requests, **Then** response times are under 2 seconds for initial page load

---

### User Story 3 - Data Maintenance Through Git (Priority: P3)

Maintainers can update competency data by modifying `competencies.yaml` and committing changes through git. Data changes are reviewed through pull requests just like code changes, with full version history and rollback capability.

**Why this priority**: Provides git-based workflow benefits while maintaining the existing YAML structure. Depends on P1 working first.

**Independent Test**: Modify `competencies.yaml` to add a new skill, commit change, restart application, verify new skill appears in competency matrix. Use `git log` to see change history, checkout previous commit to verify rollback capability.

**Acceptance Scenarios**:

1. **Given** `competencies.yaml` is updated with new skill definitions, **When** application is restarted, **Then** new skills appear in the competency matrix
2. **Given** competency data changes are committed to git, **When** reviewing pull request, **Then** YAML changes are visible in standard diff view
3. **Given** problematic data changes were merged, **When** checking out previous commit and restarting, **Then** application uses the historical data state from that commit

---

### User Story 4 - Test Data Isolation (Priority: P4)

Automated tests run with isolated in-memory database instances, ensuring test independence and fast execution. No test containers or external database services required for test runs.

**Why this priority**: Improves developer experience and CI/CD pipeline speed. Nice-to-have enhancement that builds on P1.

**Independent Test**: Run `mvn test`, verify all tests pass without requiring PostgreSQL or test containers, verify test execution completes in under 30 seconds.

**Acceptance Scenarios**:

1. **Given** test suite is executed, **When** tests run, **Then** each test uses an isolated in-memory database instance
2. **Given** CI pipeline runs tests, **When** no external services are available, **Then** all tests pass successfully
3. **Given** developer runs tests locally, **When** test execution completes, **Then** total test time is under 30 seconds

---

### Edge Cases

- What happens when `competencies.yaml` is corrupted or contains invalid data? Application should fail fast at startup with clear error message indicating YAML parsing failure and specific validation errors.
- How does system handle concurrent database access during high load? In-memory database should handle concurrent read operations safely; write operations only occur during startup sync.
- What happens when Flyway migration fails on in-memory database? Application should fail to start with clear error message indicating migration failure, ensuring database schema is always in valid state.
- What happens when sync mode is set to NONE and in-memory database is empty? Application should start with empty database, allowing manual data entry through future admin UI if needed.
- How does system handle data loss on application restart with in-memory database? This is expected behavior - data is reloaded from YAML on each startup, ensuring YAML remains the source of truth.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST use an in-memory SQL database that supports standard SQL and JDBC connectivity
- **FR-002**: System MUST automatically create and initialize in-memory database on application startup
- **FR-003**: System MUST apply all existing Flyway schema migrations to in-memory database identically to PostgreSQL
- **FR-004**: System MUST load all competency data from `competencies.yaml` into in-memory database using existing CompetencySyncService
- **FR-005**: System MUST support all three sync modes (REPLACE, MERGE, NONE) with in-memory database
- **FR-006**: System MUST maintain all existing repository methods without modification to SQL queries or data access patterns
- **FR-007**: System MUST persist all competency entities (categories, skills, roles, requirements, progressions) in in-memory database
- **FR-008**: System MUST maintain referential integrity constraints and cascade delete behavior in in-memory database
- **FR-009**: System MUST support concurrent read operations from multiple HTTP request handler threads
- **FR-010**: System MUST reload data from YAML on each application restart, ensuring YAML is the single source of truth
- **FR-011**: System MUST complete application startup including data loading in under 5 seconds
- **FR-012**: System MUST provide clear logging at startup indicating in-memory database is being used
- **FR-013**: System MUST support test isolation by providing separate in-memory database instances per test class or suite
- **FR-014**: System MUST remove all PostgreSQL connection configuration requirements from default application properties
- **FR-015**: System MUST function identically to current PostgreSQL implementation from end-user perspective (no UI or API changes)

### Key Entities *(include if feature involves data)*

- **In-Memory Database**: Temporary SQL database created in application memory, automatically initialized on startup, destroyed on shutdown, reloaded from YAML each restart
- **YAML Seed Data**: Existing `competencies.yaml` file serving as single source of truth for all competency data, version-controlled in git, loaded via CompetencySyncService
- **Database Configuration**: Application properties that specify database connection parameters, will be simplified to remove PostgreSQL-specific settings
- **Flyway Migrations**: Existing schema migration scripts (`V1__initial_schema.sql`, `V2__add_role_grouping.sql`) that must work identically on in-memory database

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: New developer can clone repository and run application successfully in under 2 minutes without installing any external services
- **SC-002**: Application startup time from process start to HTTP server ready is under 5 seconds
- **SC-003**: All existing integration tests pass without modification and without requiring external database or test containers
- **SC-004**: Test suite execution completes in under 30 seconds (faster than current test container-based approach)
- **SC-005**: Deployment to hosting platform requires zero database-specific configuration (no connection strings, credentials, or service provisioning)
- **SC-006**: Application memory usage with full dataset loaded is under 100MB additional overhead compared to PostgreSQL version
- **SC-007**: Competency matrix page load time is under 2 seconds, matching or improving on current PostgreSQL performance
- **SC-008**: Data integrity validation shows 100% accuracy when comparing in-memory database content to YAML seed data after sync
