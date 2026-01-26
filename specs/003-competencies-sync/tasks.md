# Tasks: Competencies Data Synchronization

**Input**: Design documents from `/specs/003-competencies-sync/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Constitution mandates Test-First Development (TDD). All tests MUST be written before implementation code.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Configuration and data structures needed by all user stories

- [x] T001 [P] Create SyncMode enum in src/main/java/nl/leonw/competencymatrix/config/SyncMode.java
- [x] T002 [P] Create YamlCompetencyData records in src/main/java/nl/leonw/competencymatrix/config/YamlCompetencyData.java
- [x] T003 [P] Create SyncResult record in src/main/java/nl/leonw/competencymatrix/config/SyncResult.java
- [x] T004 Add competency.sync.mode property to src/main/resources/application.properties

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Repository Enhancements

- [x] T005 [P] Write test for findByNameIgnoreCase in src/test/java/nl/leonw/competencymatrix/repository/CategoryRepositoryTest.java
- [x] T006 [P] Implement findByNameIgnoreCase in src/main/java/nl/leonw/competencymatrix/repository/CategoryRepository.java
- [x] T007 [P] Write test for findByNameAndCategoryIdIgnoreCase in src/test/java/nl/leonw/competencymatrix/repository/SkillRepositoryTest.java
- [x] T008 [P] Implement findByNameAndCategoryIdIgnoreCase in src/main/java/nl/leonw/competencymatrix/repository/SkillRepository.java
- [x] T009 [P] Write test for findByNameIgnoreCase in src/test/java/nl/leonw/competencymatrix/repository/RoleRepositoryTest.java
- [x] T010 [P] Implement findByNameIgnoreCase in src/main/java/nl/leonw/competencymatrix/repository/RoleRepository.java
- [x] T011 [P] Write test for deleteAll in src/test/java/nl/leonw/competencymatrix/repository/RoleSkillRequirementRepositoryTest.java
- [x] T012 [P] Implement deleteAll in src/main/java/nl/leonw/competencymatrix/repository/RoleSkillRequirementRepository.java
- [x] T013 [P] Write test for deleteAll in src/test/java/nl/leonw/competencymatrix/repository/RoleProgressionRepositoryTest.java
- [x] T014 [P] Implement deleteAll in src/main/java/nl/leonw/competencymatrix/repository/RoleProgressionRepository.java

### Core Sync Service

- [x] T015 Write test for YAML parsing in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [x] T016 Create CompetencySyncService class in src/main/java/nl/leonw/competencymatrix/config/CompetencySyncService.java
- [x] T017 Implement YAML parsing method (parseYaml) in CompetencySyncService
- [x] T018 Write test for YAML validation in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [x] T019 Implement YAML validation method (validateYaml) in CompetencySyncService
- [x] T020 Write test for string normalization in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [x] T021 Implement string normalization method (normalize) in CompetencySyncService

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Update Existing Competencies Without Data Loss (Priority: P1) 🎯 MVP

**Goal**: Enable administrators to incrementally update competency data via YAML file with merge mode, preserving all existing data not explicitly changed

**Independent Test**: Modify existing skill level description in competencies.yaml, restart with sync mode=merge, verify database reflects change while other data remains intact

### Tests for User Story 1 (TDD - Write FIRST, Ensure they FAIL)

- [ ] T022 [P] [US1] Write unit test for merge mode config validation in src/test/java/nl/leonw/competencymatrix/config/SyncConfigurationTest.java
- [ ] T023 [P] [US1] Write unit test for category merge logic in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [ ] T024 [P] [US1] Write unit test for skill merge logic in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [ ] T025 [P] [US1] Write unit test for role merge logic in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [ ] T026 [P] [US1] Write integration test for merge sync with existing data in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [ ] T027 [P] [US1] Write integration test for adding new skill in merge mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [ ] T028 [P] [US1] Write integration test for updating skill descriptions in merge mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [ ] T029 [P] [US1] Write integration test for adding new category in merge mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java

### Implementation for User Story 1

- [ ] T030 [US1] Implement config property reading in CompetencySyncService with @ConfigProperty injection
- [ ] T031 [US1] Implement merge mode entry point method (syncMerge) in CompetencySyncService
- [ ] T032 [US1] Implement category merge logic (processCategories) in CompetencySyncService
- [ ] T033 [US1] Implement skill merge logic (processSkills) in CompetencySyncService
- [ ] T034 [US1] Implement role merge logic (processRoles) in CompetencySyncService
- [ ] T035 [US1] Implement requirement merge logic (processRequirements) in CompetencySyncService
- [ ] T036 [US1] Implement progression merge logic (processProgressions) in CompetencySyncService
- [ ] T037 [US1] Implement entity comparison logic (needsUpdate methods) in CompetencySyncService
- [ ] T038 [US1] Implement individual operation logging in CompetencySyncService
- [ ] T039 [US1] Implement SyncResult aggregation and summary logging in CompetencySyncService
- [ ] T040 [US1] Refactor DataSeeder to delegate to CompetencySyncService in src/main/java/nl/leonw/competencymatrix/config/DataSeeder.java
- [ ] T041 [US1] Add @Transactional annotation to sync orchestrator method in CompetencySyncService

**Checkpoint**: At this point, merge mode should be fully functional - administrators can update YAML and see changes synced incrementally

---

## Phase 4: User Story 2 - Full Database Replacement from YAML (Priority: P2)

**Goal**: Enable administrators to completely reset competency database to match YAML file using replace mode

**Independent Test**: Modify competencies.yaml significantly, restart with sync mode=replace, verify database matches YAML exactly with no previous data remaining

### Tests for User Story 2 (TDD - Write FIRST, Ensure they FAIL)

- [x] T042 [P] [US2] Write unit test for replace mode config validation in src/test/java/nl/leonw/competencymatrix/config/SyncConfigurationTest.java
- [x] T043 [P] [US2] Write unit test for entity deletion order in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java
- [x] T044 [P] [US2] Write integration test for replace mode with existing data in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [x] T045 [P] [US2] Write integration test for removing roles in replace mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [x] T046 [P] [US2] Write integration test for fresh database seeding in replace mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java

### Implementation for User Story 2

- [x] T047 [US2] Implement replace mode entry point method (syncReplace) in CompetencySyncService
- [x] T048 [US2] Implement entity deletion in reverse dependency order (deleteAllEntities) in CompetencySyncService
- [x] T049 [US2] Implement fresh seeding after deletion (seedFromYaml) in CompetencySyncService reusing merge logic
- [x] T050 [US2] Add logging for deletion operations in CompetencySyncService
- [x] T051 [US2] Update DataSeeder to handle replace mode in src/main/java/nl/leonw/competencymatrix/config/DataSeeder.java

**Checkpoint**: At this point, both merge and replace modes should work independently - administrators can choose incremental or full reset

---

## Phase 5: User Story 3 - Skip Sync When Not Needed (Priority: P3)

**Goal**: Enable administrators to disable automatic sync in production environments using none mode

**Independent Test**: Set sync mode=none, modify competencies.yaml, restart, verify database remains unchanged

### Tests for User Story 3 (TDD - Write FIRST, Ensure they FAIL)

- [ ] T052 [P] [US3] Write unit test for none mode config validation in src/test/java/nl/leonw/competencymatrix/config/SyncConfigurationTest.java
- [ ] T053 [P] [US3] Write unit test for missing property default behavior in src/test/java/nl/leonw/competencymatrix/config/SyncConfigurationTest.java
- [ ] T054 [P] [US3] Write integration test for none mode skipping sync in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [ ] T055 [P] [US3] Write integration test for empty database with none mode in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java

### Implementation for User Story 3

- [ ] T056 [US3] Implement none mode early return logic in CompetencySyncService
- [ ] T057 [US3] Implement WARNING log for missing property in CompetencySyncService
- [ ] T058 [US3] Implement INFO log for explicitly set none mode in CompetencySyncService
- [ ] T059 [US3] Update DataSeeder to handle none mode in src/main/java/nl/leonw/competencymatrix/config/DataSeeder.java

**Checkpoint**: All three sync modes should now be independently functional - administrators have full control over sync behavior

---

## Phase 6: Edge Cases & Error Handling

**Purpose**: Robust error handling for validation failures, referential integrity issues, and configuration errors

### Tests for Edge Cases (TDD - Write FIRST, Ensure they FAIL)

- [ ] T060 [P] Write test for malformed YAML syntax error in src/test/java/nl/leonw/competencymatrix/validation/SyncValidationTest.java
- [ ] T061 [P] Write test for missing required YAML fields in src/test/java/nl/leonw/competencymatrix/validation/SyncValidationTest.java
- [ ] T062 [P] Write test for invalid sync mode value in src/test/java/nl/leonw/competencymatrix/validation/SyncConfigurationTest.java
- [ ] T063 [P] Write test for missing skill reference in requirement in src/test/java/nl/leonw/competencymatrix/validation/SyncValidationTest.java
- [ ] T064 [P] Write test for missing role reference in progression in src/test/java/nl/leonw/competencymatrix/validation/SyncValidationTest.java
- [ ] T065 [P] Write test for transaction rollback on failure in src/test/java/nl/leonw/competencymatrix/integration/CompetencySyncIntegrationTest.java
- [ ] T066 [P] Write test for missing competencies.yaml file in src/test/java/nl/leonw/competencymatrix/validation/SyncValidationTest.java
- [ ] T067 [P] Write test for case-insensitive skill lookup in requirement processing in src/test/java/nl/leonw/competencymatrix/config/CompetencySyncServiceTest.java

### Implementation for Edge Cases

- [ ] T068 Implement YAML syntax error handling with clear messages in CompetencySyncService
- [ ] T069 Implement structural validation error handling in CompetencySyncService
- [ ] T070 Implement invalid sync mode error with valid value list in CompetencySyncService
- [ ] T071 Implement referential integrity check for skill references in CompetencySyncService
- [ ] T072 Implement case-insensitive skill lookup in YAML during requirement processing in CompetencySyncService
- [ ] T073 Implement referential integrity check for role references in CompetencySyncService
- [ ] T074 Implement missing YAML file handling per sync mode in CompetencySyncService
- [ ] T075 Add comprehensive error logging for all failure scenarios in CompetencySyncService

**Checkpoint**: All edge cases handled robustly - system fails fast with clear messages, leaving database consistent

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, performance validation, and final quality checks

- [ ] T076 [P] Update README.md with sync mode configuration instructions
- [ ] T077 [P] Create example competencies.yaml files for testing in src/test/resources/
- [ ] T078 [P] Add configuration examples to quickstart.md
- [ ] T079 [P] Write performance test for 100 skills + 20 roles in src/test/java/nl/leonw/competencymatrix/validation/SyncPerformanceTest.java
- [ ] T080 Run performance test and verify <5 second requirement
- [ ] T081 Run all tests and verify 100% pass rate
- [ ] T082 Run application with dev mode and verify startup logs
- [ ] T083 Validate quickstart.md scenarios manually
- [ ] T084 Code review for Constitution compliance (Simplicity First, Data Integrity)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-5)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Edge Cases (Phase 6)**: Can proceed in parallel with or after user stories
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Reuses merge logic from US1 but is independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Independent of US1/US2

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD per Constitution)
- Unit tests before implementation
- Integration tests verify end-to-end behavior
- Implementation follows test completion
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks (T001-T004) can run in parallel
- All Repository tests/implementations marked [P] in Phase 2 can run in parallel
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests within a user story marked [P] can run in parallel
- Edge case tests (T060-T067) can run in parallel
- Polish tasks (T076-T078) can run in parallel

---

## Parallel Example: User Story 1 Tests

```bash
# Launch all unit tests for User Story 1 together:
Task T023: "Write unit test for category merge logic"
Task T024: "Write unit test for skill merge logic"
Task T025: "Write unit test for role merge logic"

# Launch all integration tests for User Story 1 together:
Task T026: "Write integration test for merge sync with existing data"
Task T027: "Write integration test for adding new skill in merge mode"
Task T028: "Write integration test for updating skill descriptions in merge mode"
Task T029: "Write integration test for adding new category in merge mode"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001-T004)
2. Complete Phase 2: Foundational (T005-T021) - CRITICAL - blocks all stories
3. Complete Phase 3: User Story 1 (T022-T041)
4. **STOP and VALIDATE**: Test User Story 1 independently with merge mode
5. Deploy/demo merge mode capability

**MVP Deliverable**: Administrators can update competencies.yaml and see incremental database updates on application restart with merge mode.

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready (T001-T021)
2. Add User Story 1 → Test independently → Deploy/Demo (MVP - merge mode working!)
3. Add User Story 2 → Test independently → Deploy/Demo (replace mode added)
4. Add User Story 3 → Test independently → Deploy/Demo (none mode for production safety)
5. Add Edge Cases (Phase 6) → Robust error handling
6. Polish (Phase 7) → Production-ready documentation

Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together (T001-T021)
2. Once Foundational is done:
   - Developer A: User Story 1 tests + implementation (T022-T041)
   - Developer B: User Story 2 tests + implementation (T042-T051)
   - Developer C: User Story 3 tests + implementation (T052-T059)
   - Developer D: Edge Cases tests + implementation (T060-T075)
3. Stories complete and integrate independently
4. Team completes Polish together (T076-T084)

---

## Task Statistics

**Total Tasks**: 84
**By Phase**:
- Phase 1 (Setup): 4 tasks
- Phase 2 (Foundational): 17 tasks (11 TDD pairs + 6 core service)
- Phase 3 (User Story 1): 20 tasks (8 tests + 12 implementation)
- Phase 4 (User Story 2): 10 tasks (5 tests + 5 implementation)
- Phase 5 (User Story 3): 8 tasks (4 tests + 4 implementation)
- Phase 6 (Edge Cases): 16 tasks (8 tests + 8 implementation)
- Phase 7 (Polish): 9 tasks

**Parallel Opportunities**: 45 tasks marked [P] can run in parallel (53% of tasks)

**MVP Scope**: 41 tasks (Setup + Foundational + US1) delivers working merge mode

**Test-First Ratio**: 36 test tasks, 48 implementation tasks (75% TDD coverage)

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- **TDD MANDATE**: Verify tests fail before implementing (Constitution requirement)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- All tasks follow strict checklist format: `- [ ] T### [P?] [Story?] Description with file path`
