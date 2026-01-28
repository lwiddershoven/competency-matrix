# Tasks: Multi-File Competencies Data

**Input**: Design documents from `/specs/006-competencies-split/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Following Constitution Principle II (Test-First Development), all tests are written BEFORE implementation code.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Project structure: Single monolith (backend + server-side rendered frontend)
- **Source**: `src/main/java/nl/leonw/competencymatrix/`
- **Resources**: `src/main/resources/`
- **Tests**: `src/test/java/nl/leonw/competencymatrix/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create split YAML files and verify structure

- [ ] T001 Create categories directory at src/main/resources/seed/categories/
- [ ] T002 Create roles directory at src/main/resources/seed/roles/
- [ ] T003 [P] Create migration script to split competencies.yaml into category files in scripts/split-competencies.sh
- [ ] T004 [P] Verify migration script preserves all data from original competencies.yaml

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: File discovery and merging infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 Add file discovery helper method discoverYamlFiles(String directoryPath) to CompetencySyncService.java
- [ ] T006 Add filename-to-entity mapping helper method parseYamlFile(Path filePath) to CompetencySyncService.java
- [ ] T007 Add duplicate detection helper method detectDuplicateCategories(List<CategoryData>, Map<String, String>) to CompetencySyncService.java
- [ ] T008 Add duplicate detection helper method detectDuplicateRoles(List<RoleData>, Map<String, String>) to CompetencySyncService.java

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Category File Editing (Priority: P1) 🎯 MVP

**Goal**: Business editors can edit individual category files without navigating through 678-line monolithic file

**Independent Test**: Edit programming.yaml, restart application, verify ONLY Programming category changes appear in UI while other categories remain unchanged

### Tests for User Story 1 (TDD Approach)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T009 [P] [US1] Unit test: discoverYamlFiles() returns empty list for missing directory in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [ ] T010 [P] [US1] Unit test: discoverYamlFiles() returns sorted file paths for categories/ directory in CompetencySyncServiceTest.java
- [ ] T011 [P] [US1] Unit test: discoverYamlFiles() filters non-YAML files in CompetencySyncServiceTest.java
- [ ] T012 [P] [US1] Integration test: Load programming.yaml, verify CategoryData("Programming", skills) in CompetencySyncServiceTest.java
- [ ] T013 [P] [US1] Integration test: Load all category files, verify 6 categories merged correctly in CompetencySyncServiceTest.java
- [ ] T014 [P] [US1] Integration test: Empty category file returns valid CategoryData with empty skills list in CompetencySyncServiceTest.java
- [ ] T015 [P] [US1] Integration test: Duplicate category names across files throws IllegalStateException with both filenames in CompetencySyncServiceTest.java

### Implementation for User Story 1

- [ ] T016 [US1] Execute migration script to create category YAML files: programming.yaml, software-design.yaml, devops-infrastructure.yaml, quality-testing.yaml, soft-skills.yaml, architecture-frameworks.yaml in src/main/resources/seed/categories/
- [ ] T017 [US1] Implement discoverYamlFiles() using Files.list() with try-with-resources per research.md in CompetencySyncService.java
- [ ] T018 [US1] Implement detectDuplicateCategories() using HashMap tracking per research.md in CompetencySyncService.java
- [ ] T019 [US1] Modify loadYamlData() to discover category files from categories/ directory in CompetencySyncService.java:300-310
- [ ] T020 [US1] Modify loadYamlData() to loop through category files and merge into List<CategoryData> using List.addAll() in CompetencySyncService.java
- [ ] T021 [US1] Add per-file error reporting with filename context when YAML parse fails in CompetencySyncService.java
- [ ] T022 [US1] Add logging for category file discovery (info level) and missing directory (warn level) in CompetencySyncService.java
- [ ] T023 [US1] Verify all unit tests pass (T009-T011)
- [ ] T024 [US1] Verify all integration tests pass (T012-T015)

**Checkpoint**: At this point, category multi-file loading should be fully functional and independently testable

---

## Phase 4: User Story 2 - Role File Editing (Priority: P2)

**Goal**: Business editors can edit individual role files independently from category files

**Independent Test**: Edit junior-developer.yaml, restart application, verify ONLY Junior Developer role changes appear while categories and other roles remain unchanged

### Tests for User Story 2 (TDD Approach)

- [ ] T025 [P] [US2] Integration test: Load junior-developer.yaml, verify RoleData("Junior Developer", requirements) in CompetencySyncServiceTest.java
- [ ] T026 [P] [US2] Integration test: Load all role files, verify 9 roles merged correctly in CompetencySyncServiceTest.java
- [ ] T027 [P] [US2] Integration test: Empty role file returns valid RoleData with empty requirements list in CompetencySyncServiceTest.java
- [ ] T028 [P] [US2] Integration test: Duplicate role names across files throws IllegalStateException with both filenames in CompetencySyncServiceTest.java
- [ ] T029 [P] [US2] Integration test: Role requirements reference valid categories and skills from category files in CompetencySyncServiceTest.java

### Implementation for User Story 2

- [ ] T030 [US2] Execute migration script to create role YAML files: junior-developer.yaml, medior-developer.yaml, senior-developer.yaml, specialist-developer.yaml, lead-developer.yaml, lead-developer-software-architect.yaml, software-architect.yaml, solution-architect.yaml, devops-engineer.yaml in src/main/resources/seed/roles/
- [ ] T031 [US2] Implement detectDuplicateRoles() using HashMap tracking per research.md in CompetencySyncService.java
- [ ] T032 [US2] Modify loadYamlData() to discover role files from roles/ directory in CompetencySyncService.java
- [ ] T033 [US2] Modify loadYamlData() to loop through role files and merge into List<RoleData> using List.addAll() in CompetencySyncService.java
- [ ] T034 [US2] Add logging for role file discovery (info level) and missing directory (warn level) in CompetencySyncService.java
- [ ] T035 [US2] Verify all integration tests pass (T025-T029)

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently (categories and roles load from separate files)

---

## Phase 5: User Story 3 - Validation on Load (Priority: P3)

**Goal**: System administrators get clear error messages identifying specific files with syntax or schema errors on startup

**Independent Test**: Introduce intentional YAML syntax error in programming.yaml, restart application, verify error message shows "programming.yaml" and line number

### Tests for User Story 3 (TDD Approach)

- [ ] T036 [P] [US3] Integration test: Invalid YAML syntax in category file logs error with filename and line number in CompetencySyncServiceTest.java
- [ ] T037 [P] [US3] Integration test: Invalid YAML syntax in role file logs error with filename and line number in CompetencySyncServiceTest.java
- [ ] T038 [P] [US3] Integration test: Missing required field in category file reports filename and field name in CompetencySyncServiceTest.java
- [ ] T039 [P] [US3] Integration test: Missing required field in role file reports filename and field name in CompetencySyncServiceTest.java
- [ ] T040 [P] [US3] Integration test: Schema mismatch (valid YAML, wrong structure) reports filename in error in CompetencySyncServiceTest.java

### Implementation for User Story 3

- [ ] T041 [US3] Enhance parseYamlFile() to wrap SnakeYAML exceptions with filename context in CompetencySyncService.java
- [ ] T042 [US3] Enhance validateYaml() to include filename in validation error messages in CompetencySyncService.java:127-193
- [ ] T043 [US3] Update error logging to include file path in all error messages in CompetencySyncService.java
- [ ] T044 [US3] Add structured error reporting with filename, line number, and error type in CompetencySyncService.java
- [ ] T045 [US3] Verify all integration tests pass (T036-T040)

**Checkpoint**: All user stories should now be independently functional with enhanced error reporting

---

## Phase 6: Progressions File Support

**Goal**: Support separate progressions.yaml file as requested by user

**Independent Test**: Create progressions.yaml with progression data, restart application, verify progressions load correctly

### Tests for Progressions (TDD Approach)

- [ ] T046 [P] Integration test: Load progressions.yaml, verify List<ProgressionData> contains all progressions in CompetencySyncServiceTest.java
- [ ] T047 [P] Integration test: Missing progressions.yaml logs warning and returns empty list in CompetencySyncServiceTest.java
- [ ] T048 [P] Integration test: Invalid progression references non-existent role and validation fails in CompetencySyncServiceTest.java

### Implementation for Progressions

- [ ] T049 Execute migration script to create progressions.yaml from progressions section of competencies.yaml in src/main/resources/seed/
- [ ] T050 Modify loadYamlData() to load progressions from progressions.yaml file in CompetencySyncService.java
- [ ] T051 Add logging for progressions file discovery and parsing in CompetencySyncService.java
- [ ] T052 Verify all progressions integration tests pass (T046-T048)

**Checkpoint**: Progressions now load from separate file, completing the multi-file split

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, cleanup, and final validation

- [ ] T053 [P] Update README.md with new file structure and editing instructions
- [ ] T054 [P] Verify quickstart.md instructions work end-to-end by following guide
- [ ] T055 [P] Add inline code comments documenting file discovery and merging logic in CompetencySyncService.java
- [ ] T056 Run full integration test suite to verify zero data loss (compare split files to original competencies.yaml)
- [ ] T057 Measure and verify application startup time remains under 2 seconds (Success Criterion SC-002)
- [ ] T058 [P] Archive original competencies.yaml to src/main/resources/seed/archive/competencies.yaml.backup for reference
- [ ] T059 Code review: Verify Constitution compliance (Simplicity First, TDD, Data Integrity)
- [ ] T060 Final validation: Edit one category file, one role file, restart, verify changes reflected correctly

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User Story 1 (Category Files): Can start after Foundational - No dependencies on other stories
  - User Story 2 (Role Files): Can start after Foundational - No dependencies on US1 (independent)
  - User Story 3 (Validation): Can start after Foundational - Enhances US1 & US2 but independent
- **Progressions (Phase 6)**: Depends on Foundational phase - Independent of user stories
- **Polish (Phase 7)**: Depends on all user stories and progressions being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Independent of US1 (can run in parallel)
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2 (can run in parallel)
- **Progressions**: Can start after Foundational (Phase 2) - Independent of all user stories

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD cycle)
- File discovery before loading
- Loading before validation
- Core implementation before error handling
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1 (Setup)**: T003 and T004 can run in parallel
- **Phase 2 (Foundational)**: All tasks sequential (shared file modifications)
- **Phase 3 (US1 Tests)**: T009-T015 can all run in parallel (different test methods)
- **Phase 4 (US2 Tests)**: T025-T029 can all run in parallel
- **Phase 5 (US3 Tests)**: T036-T040 can all run in parallel
- **Phase 6 (Progressions Tests)**: T046-T048 can run in parallel
- **Phase 7 (Polish)**: T053, T054, T055, T058 can run in parallel
- **User Stories**: After Foundational, US1, US2, US3, and Progressions can be implemented in parallel by different developers

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (TDD - write all tests first):
Task: "Unit test: discoverYamlFiles() returns empty list for missing directory"
Task: "Unit test: discoverYamlFiles() returns sorted file paths for categories/"
Task: "Unit test: discoverYamlFiles() filters non-YAML files"
Task: "Integration test: Load programming.yaml, verify CategoryData"
Task: "Integration test: Load all category files, verify 6 categories merged"
Task: "Integration test: Empty category file returns valid CategoryData"
Task: "Integration test: Duplicate category names throws IllegalStateException"

# Then implement (after all tests are written and failing):
Task: "Execute migration script to create category YAML files"
Task: "Implement discoverYamlFiles() using Files.list()"
Task: "Implement detectDuplicateCategories() using HashMap"
# ... etc.
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (create directories, migration script)
2. Complete Phase 2: Foundational (file discovery helpers)
3. Complete Phase 3: User Story 1 (category file loading)
4. **STOP and VALIDATE**: Test category file editing independently
5. Deploy/demo if ready

**At this point**: Business editors can edit category files independently (core value delivered)

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP - category files!)
3. Add User Story 2 → Test independently → Deploy/Demo (add role files!)
4. Add User Story 3 → Test independently → Deploy/Demo (add validation!)
5. Add Progressions → Test independently → Deploy/Demo (complete split!)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (sequential work)
2. Once Foundational is done:
   - Developer A: User Story 1 (Category Files)
   - Developer B: User Story 2 (Role Files)
   - Developer C: User Story 3 (Validation Enhancement)
   - Developer D: Progressions Support
3. Stories complete and integrate independently
4. All converge for Phase 7 (Polish)

---

## Test-Driven Development Workflow (Constitution Compliant)

Following Constitution Principle II (Test-First Development - NON-NEGOTIABLE):

### Red-Green-Refactor Cycle

For EACH task with tests:

1. **RED**: Write test(s) first, run them, verify they FAIL
   - Example: T009-T015 (User Story 1 tests) must all fail initially
2. **GREEN**: Write minimum code to make tests pass
   - Example: T017-T022 (User Story 1 implementation) makes tests pass
3. **REFACTOR**: Clean up code while keeping tests green
   - Example: T023-T024 (verify tests still pass after refactoring)

### Task Execution Order (Per User Story)

```
User Story 1 Example:
1. T009-T015: Write ALL tests (they all fail) ← RED
2. T016: Prepare data files
3. T017-T022: Implement features one by one ← GREEN (tests start passing)
4. T023-T024: Verify and refactor ← GREEN (all tests pass)
```

### Test Coverage Requirements

- Unit tests for all helper methods (discoverYamlFiles, detectDuplicates, etc.)
- Integration tests for file loading and merging
- Integration tests for ALL edge cases (empty files, duplicates, missing dirs, invalid YAML)
- Integration tests for error reporting (filename in error messages)
- End-to-end test for complete workflow (T060)

---

## File Path Reference

| Task Type | File Path |
|-----------|-----------|
| Source code changes | `src/main/java/nl/leonw/competencymatrix/config/CompetencySyncService.java` |
| Test code | `src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java` |
| Category files | `src/main/resources/seed/categories/{category-name}.yaml` |
| Role files | `src/main/resources/seed/roles/{role-name}.yaml` |
| Progressions file | `src/main/resources/seed/progressions.yaml` |
| Migration script | `scripts/split-competencies.sh` |
| Documentation | `README.md`, `specs/006-competencies-split/quickstart.md` |

---

## Notes

- **[P]** tasks = different files or test methods, no dependencies
- **[Story]** label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- **CRITICAL**: Verify tests FAIL before implementing (TDD requirement)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Total estimated effort: ~65 lines of new code + ~150 lines of test code
- Expected completion: LOW complexity (2-3 days for single developer doing MVP)

---

## Success Criteria Verification

Map tasks to Success Criteria from spec.md:

- **SC-001** (Files under 100 lines): Verified in T001-T004 (migration script)
- **SC-002** (Startup < 2 seconds): Measured in T057
- **SC-003** (Zero data loss): Verified in T056
- **SC-004** (Error reporting < 5 seconds): Verified in T036-T044
- **SC-005** (Independent editing): Verified in T060

All success criteria have corresponding validation tasks.
