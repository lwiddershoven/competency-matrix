# Feature Specification: Competencies Data Synchronization

**Feature Branch**: `003-competencies-sync`
**Created**: 2026-01-26
**Status**: Draft
**Input**: User description: "If an update of the data file competencies.yaml occurs these updates should be incorporated in the database. Based on a property this should either replace the current database content or be added to where changed."

## Clarifications

### Session 2026-01-26

- Q: In merge mode, when a category name exists in both database and YAML but has different display order, how should the system behave? → A: Update display order to match YAML
- Q: What should the system do when the sync mode configuration property is missing (not set in application.properties)? → A: Default to "none" with WARNING log message when property is missing
- Q: When competencies.yaml contains invalid data (malformed YAML or missing required fields), should the system fail immediately or log errors and continue with empty data? → A: Fail startup with clear error message and log validation errors; log each individual sync operation and aggregate summary
- Q: In merge mode, when a role requirement references a skill that doesn't exist in the database yet, what should happen? → A: Check YAML for skill first (case/space-insensitive), save if found; if not in database or YAML, log ERROR and fail startup
- Q: When the sync mode property is set to an invalid value (not "none", "merge", or "replace"), what should the system do? → A: Fail startup with clear error message that includes the valid values

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Update Existing Competencies Without Data Loss (Priority: P1)

A system administrator updates the competencies.yaml file to fix skill level descriptions or add new skills to existing categories. When the application restarts, these changes should be incorporated into the database without losing any existing data that wasn't modified.

**Why this priority**: This is the core value proposition - allowing competency data to evolve over time without requiring manual database updates or risking data loss. This is the most common use case.

**Independent Test**: Can be fully tested by modifying an existing skill's level description in the YAML file, restarting the application, and verifying the database reflects the change while other data remains intact. Delivers immediate value by enabling safe competency updates.

**Acceptance Scenarios**:

1. **Given** the database contains existing competency data, **When** an administrator modifies a skill level description in competencies.yaml and restarts the application with sync mode set to "merge", **Then** the skill level description is updated in the database and all other data remains unchanged
2. **Given** the database contains existing competency data, **When** an administrator adds a new skill to an existing category in competencies.yaml and restarts the application with sync mode set to "merge", **Then** the new skill is added to the database and existing skills are preserved
3. **Given** the database contains existing competency data, **When** an administrator adds a new category with skills in competencies.yaml and restarts the application with sync mode set to "merge", **Then** the new category and its skills are added to the database without affecting existing categories

---

### User Story 2 - Full Database Replacement from YAML (Priority: P2)

A system administrator needs to completely reset the competency database to match a new or corrected version of competencies.yaml. This might be needed during development, testing, or when migrating to a new competency framework.

**Why this priority**: Important for specific scenarios (testing, major framework changes) but less common than incremental updates. Users need explicit control to prevent accidental data loss.

**Independent Test**: Can be fully tested by modifying competencies.yaml significantly, restarting the application with sync mode set to "replace", and verifying the database matches the YAML file exactly with no previous data remaining. Delivers value for clean-slate scenarios.

**Acceptance Scenarios**:

1. **Given** the database contains existing competency data, **When** an administrator modifies competencies.yaml and restarts the application with sync mode set to "replace", **Then** all existing competency data is deleted and the database is populated fresh from the YAML file
2. **Given** the database contains existing competency data, **When** an administrator removes a role from competencies.yaml and restarts the application with sync mode set to "replace", **Then** the removed role no longer exists in the database
3. **Given** an empty or new database, **When** the application starts with competencies.yaml present and sync mode set to "replace", **Then** the database is populated from the YAML file (same behavior as initial seeding)

---

### User Story 3 - Skip Sync When Not Needed (Priority: P3)

A system administrator deploys the application to a production environment where the competency data should not be automatically modified. The sync behavior should be disabled to prevent unintended changes.

**Why this priority**: Safety feature for production environments, but optional since the merge mode is designed to be safe. Lower priority as it's more about operational control than core functionality.

**Independent Test**: Can be fully tested by setting sync mode to "none", modifying competencies.yaml, restarting the application, and verifying the database remains unchanged. Delivers value by giving operators explicit control in sensitive environments.

**Acceptance Scenarios**:

1. **Given** the database contains existing competency data, **When** an administrator modifies competencies.yaml and restarts the application with sync mode set to "none", **Then** the database remains unchanged regardless of YAML file contents
2. **Given** an empty database, **When** the application starts with competencies.yaml present and sync mode set to "none", **Then** the database remains empty and no seeding occurs

---

### Edge Cases

- Invalid YAML data (missing required fields, malformed YAML) will cause startup failure with clear error message
- Role requirement referencing non-existent skill: System checks YAML first (case/space-insensitive), saves skill if found, otherwise logs ERROR and fails startup
- Invalid sync mode property value: System fails startup with clear error message listing valid values ("none", "merge", "replace")
- What happens when competencies.yaml is missing from the classpath?
- What happens when a role or skill name in the YAML conflicts with an existing database entry (same name but different meaning)?
- What happens when the database is in an inconsistent state (referential integrity violations)?
- How are role progressions handled when the "from" or "to" roles don't exist yet?
- What happens when a sync operation fails partway through (database errors, constraint violations)?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support three sync modes configurable via application property: "none" (no sync), "merge" (update existing/add new), and "replace" (full replacement)
- **FR-002**: System MUST read the sync mode from application.properties configuration key at startup; if the property is missing, system MUST default to "none" and log a WARNING message indicating the property is missing and defaulting to no sync; if the property value is invalid (not "none", "merge", or "replace"), system MUST fail startup with a clear error message that includes the valid values
- **FR-003**: System MUST validate competencies.yaml syntax and structure before attempting any database modifications; if validation fails, system MUST fail startup immediately with a clear error message logged to the log file
- **FR-004**: System MUST execute all sync operations within a database transaction that can be rolled back on failure
- **FR-005**: System MUST log all sync operations at INFO level for audit purposes; each individual change MUST be logged (e.g., "Role X synced", "Category Y synced") and an aggregate summary MUST be logged at completion (e.g., "Sync complete: X role updates, Y category updates, Z skill updates")
- **FR-006**: In "merge" mode, system MUST update all attributes of existing categories, skills, roles, requirements, and progressions when they match by name (e.g., category display order, skill level descriptions, role descriptions)
- **FR-007**: In "merge" mode, system MUST add new categories, skills, roles, requirements, and progressions that don't exist in the database
- **FR-008**: In "merge" mode, system MUST NOT delete any data from the database (no removals)
- **FR-009**: In "replace" mode, system MUST delete all existing competency data before loading from YAML
- **FR-010**: In "none" mode, system MUST NOT modify the database regardless of YAML file contents
- **FR-011**: System MUST handle missing competencies.yaml gracefully based on sync mode (error for replace/merge, silent for none)
- **FR-012**: System MUST provide clear error messages when sync operations fail, indicating what went wrong and what state the database is in
- **FR-013**: System MUST preserve referential integrity during all sync operations (roles reference skills, progressions reference roles, etc.); when processing role requirements, system MUST check if referenced skill exists in YAML using case-insensitive and space-insensitive matching, save the skill first if found, and fail startup with ERROR log if skill not found in either database or YAML
- **FR-014**: System MUST compare existing data to YAML data efficiently to determine what needs updating (avoid unnecessary database writes)
- **FR-015**: System MUST perform case-insensitive and space-insensitive matching when comparing entity names (categories, skills, roles) between database and YAML; spaces MUST be normalized but capital letters MUST be preserved as specified in YAML when storing to database

### Key Entities

- **Sync Configuration**: The application property that controls sync behavior (value: "none", "merge", or "replace")
- **Competency Data**: Categories, Skills, Roles, RoleSkillRequirements, and RoleProgressions as defined in competencies.yaml
- **Sync Operation Log**: Record of what was changed during each sync operation (for observability and debugging)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Administrators can update competency data by modifying a YAML file and restarting the application, without writing SQL or using database tools
- **SC-002**: Sync operations complete in under 5 seconds for a competencies.yaml file containing up to 100 skills and 20 roles
- **SC-003**: Failed sync operations leave the database in a consistent state (all changes rolled back) with clear error messages indicating the problem
- **SC-004**: Merge mode preserves 100% of existing database data that isn't explicitly updated in the YAML file
- **SC-005**: Replace mode results in a database state that exactly matches the YAML file content with no residual data
- **SC-006**: Administrators can prevent any automatic sync by setting a configuration property, ensuring production stability
- **SC-007**: All sync operations are logged with sufficient detail to understand what changed without querying the database directly