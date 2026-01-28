# Feature Specification: Multi-File Competencies Data

**Feature Branch**: `006-competencies-split`
**Created**: 2026-01-28
**Status**: Draft
**Input**: User description: "The database content is now managed in a single file competencies.yaml. With the huge amount of content in there it is hard to maintain for the business. The data should be split in multiple files, one for each of the top levels of the yaml."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Category File Editing (Priority: P1)

Business content editors need to update competency definitions within a specific category (e.g., "Programming" or "DevOps & Infrastructure") without navigating through 678 lines of unrelated content. Each category should have its own file that can be opened, edited, and saved independently.

**Why this priority**: This is the core value proposition. The main pain point is maintainability of the large monolithic file. Splitting by category immediately solves this for the most common editing workflow.

**Independent Test**: Can be fully tested by editing a single category file (e.g., programming.yaml), reloading the application, and verifying the changes appear correctly in the UI. Delivers immediate maintainability improvement.

**Acceptance Scenarios**:

1. **Given** the competencies data is split into category files, **When** a business editor opens programming.yaml, **Then** only Programming category skills and levels are visible in that file
2. **Given** multiple category files exist, **When** the application starts, **Then** all categories load correctly as they did with the single file
3. **Given** an editor modifies programming.yaml, **When** the application reloads, **Then** only Programming category data changes while other categories remain unchanged

---

### User Story 2 - Role File Editing (Priority: P2)

Business content editors need to update role definitions (e.g., "Junior Developer", "Senior Developer") independently from category skill definitions. Roles should be stored separately from categories to allow role-focused editing.

**Why this priority**: Roles and categories are conceptually different entities often edited by different stakeholders or at different times. This separation improves maintainability further but depends on P1 being completed.

**Independent Test**: Can be tested by editing a role file (e.g., junior-developer.yaml), reloading the application, and verifying role definitions update correctly without touching category files.

**Acceptance Scenarios**:

1. **Given** roles are in separate files from categories, **When** an editor opens junior-developer.yaml, **Then** only Junior Developer role data is visible
2. **Given** role files and category files exist separately, **When** the application starts, **Then** both roles and categories load correctly and can be displayed in the matrix view
3. **Given** an editor modifies a role file, **When** the application reloads, **Then** only that role's data changes while categories remain unchanged

---

### User Story 3 - Validation on Load (Priority: P3)

System administrators need to know immediately if any category or role file has syntax errors or missing required fields when the application starts, with clear error messages indicating which file has the problem.

**Why this priority**: Prevents broken data from being deployed but doesn't provide business value until P1 and P2 are complete. This is defensive quality assurance.

**Independent Test**: Can be tested by introducing intentional errors in one file and verifying the application reports the specific file and error on startup.

**Acceptance Scenarios**:

1. **Given** a category file has invalid YAML syntax, **When** the application starts, **Then** the system logs an error message identifying the specific file and line number
2. **Given** all files are valid, **When** the application starts, **Then** no errors are logged and data loads successfully
3. **Given** a required field is missing in a role file, **When** the application starts, **Then** the system reports which file and which field is missing

---

### Edge Cases

- What happens when a category file is empty or contains only whitespace? A: that's fine. Only if a category is depended on by something it needs to be there. 
- How does the system handle a missing category file that is expected to exist? A: ignore but log (warning). Maybe it is in a different file.
- What happens if duplicate category names exist across multiple files? A: log an error with the category name and the files it is in and exit the app
- How does the system handle files with different character encodings (UTF-8 vs others)? A: Assume UTF-8
- What happens if a category file exists but has no skills defined? A: that's fine. An error will occur if a skill is referred to that does not exist, not if it is not in a file. (e.g. error on dependency resolution not when loading)
- How does the system behave if the file structure is valid YAML but doesn't match the expected schema? A: Log an error and exit

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST load competency data from multiple YAML files instead of a single competencies.yaml file
- **FR-002**: System MUST create one file per top-level category (e.g., programming.yaml, software-design.yaml, devops-infrastructure.yaml)
- **FR-003**: System MUST create one file per role definition (e.g., junior-developer.yaml, senior-developer.yaml)
- **FR-004**: System MUST maintain the same data structure within each file as exists in the current single file (categories have skills, skills have levels)
- **FR-005**: System MUST validate each file independently on startup and report errors with the specific filename
- **FR-006**: System MUST merge all category and role files into a single in-memory data structure identical to the current structure
- **FR-007**: System MUST preserve all existing competency data during the migration (no data loss)
- **FR-008**: File names MUST be derived from the category/role names using lowercase with hyphens (e.g., "DevOps & Infrastructure" → devops-infrastructure.yaml)
- **FR-009**: System MUST fail fast on startup if any file is missing, malformed, or contains invalid data
- **FR-010**: System MUST maintain backward compatibility with existing database schema and application code expecting the current data structure

### Key Entities

- **Category File**: Represents a single competency category with its skills and proficiency levels (e.g., programming.yaml contains Java, Python, SQL skills). Each file contains one category with its nested skills and levels.
- **Role File**: Represents a single role definition with its requirements and expectations (e.g., junior-developer.yaml). Each file contains one role entity.
- **File Naming Convention**: Transformation rule that converts display names to filesystem-safe names (lowercase, spaces to hyphens, special characters removed).
- **Data Loader**: Component that discovers, reads, validates, and merges all category and role files into the unified in-memory structure.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Business editors can locate and edit a specific category's content by opening a single file under 100 lines instead of searching through 678 lines
- **SC-002**: Application startup time remains within 2 seconds (same as current single-file implementation)
- **SC-003**: Zero data loss during migration - all 15 categories and all roles from the original file must be present and correct after the split
- **SC-004**: File validation errors identify the specific file and error type within 5 seconds of startup attempt
- **SC-005**: Editors can independently edit categories without risk of merge conflicts in unrelated competency areas

## Assumptions *(optional)*

- The competencies.yaml file structure will remain stable (categories with skills, roles with definitions) during and after migration
- Business editors have basic file system navigation skills to open individual YAML files
- The application startup process already has YAML parsing capabilities that can be reused
- File system access performance is sufficient for reading 15+ small files instead of one large file
- The seed data directory (src/main/resources/seed/) will continue to be the location for these files
- Backward compatibility is required - the application code consuming this data should not need significant changes

## Out of Scope *(optional)*

- UI for editing YAML files (editors will continue using text editors)
- Automatic merging or conflict resolution between files
- Version control or change tracking within the YAML files themselves (Git handles this)
- Dynamic category creation or deletion at runtime (categories remain static seed data)
- Migration of historical data or versioning of the competencies structure
- Internationalization or multiple language versions of the files
- Real-time reloading of files without application restart

## Dependencies *(optional)*

- Current YAML parsing library (SnakeYAML based on codebase inspection)
- File system read access to src/main/resources/seed/ directory
- Existing data loading mechanism that currently reads competencies.yaml

## Risks & Mitigations *(optional)*

**Risk**: Data inconsistency if files are partially updated
**Mitigation**: Fail-fast validation on startup ensures all-or-nothing loading

**Risk**: Business editors accidentally delete or rename files
**Mitigation**: Version control (Git) provides recovery mechanism; clear documentation on file naming conventions

**Risk**: Performance degradation from reading multiple files
**Mitigation**: Success criterion SC-002 ensures startup time remains acceptable; files are read once at startup

**Risk**: Merge conflicts when multiple editors work on different categories simultaneously
**Mitigation**: This is actually an improvement - separate files reduce merge conflicts compared to single file

## References *(optional)*

- Current implementation: src/main/resources/seed/competencies.yaml (678 lines, 15 top-level entries)
- Categories requiring files: Programming, Software Design, DevOps & Infrastructure, Quality & Testing, Soft Skills, Architecture Frameworks
- Roles requiring files: Junior Developer, Medior Developer, Senior Developer, Specialist Developer, Lead Developer, Lead Developer / Software Architect, Software Architect, Solution Architect, DevOps Engineer
