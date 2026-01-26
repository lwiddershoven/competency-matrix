# Tasks: Competency Matrix Table Overview

**Input**: Design documents from `/specs/004-matrix-overview/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/matrix-api.yaml

**Tests**: Following Constitution Principle II (Test-First Development), tests MUST be written before implementation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

All paths are relative to repository root:
- Java source: `src/main/java/nl/leonw/competencymatrix/`
- Java tests: `src/test/java/nl/leonw/competencymatrix/`
- Resources: `src/main/resources/`
- Web assets: `src/main/resources/META-INF/resources/`

---

## Phase 1: Setup (Database & Infrastructure)

**Purpose**: Database schema changes and foundational infrastructure for matrix feature

- [ ] T001 Create database migration `src/main/resources/db/migration/V2__add_role_grouping.sql`
- [ ] T002 Run migration to add `role_family` and `seniority_order` columns to `rolename` table
- [ ] T003 [P] Update seed data `src/main/resources/seed/competencies.yaml` with `roleFamily` and `seniorityOrder` fields
- [ ] T004 [P] Update Role model to include `roleFamily` and `seniorityOrder` fields in `src/main/java/nl/leonw/competencymatrix/model/Role.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core DTOs and service methods that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T005 [P] Create MatrixViewModel DTO in `src/main/java/nl/leonw/competencymatrix/dto/MatrixViewModel.java`
- [ ] T006 [P] Create MatrixRow DTO in `src/main/java/nl/leonw/competencymatrix/dto/MatrixRow.java`
- [ ] T007 [P] Create MatrixCell DTO in `src/main/java/nl/leonw/competencymatrix/dto/MatrixCell.java`
- [ ] T008 [P] Create MatrixColumnHeader DTO in `src/main/java/nl/leonw/competencymatrix/dto/MatrixColumnHeader.java`
- [ ] T009 Write unit test for MatrixCell factory methods in `src/test/java/nl/leonw/competencymatrix/dto/MatrixCellTest.java`
- [ ] T010 [P] Write unit test for MatrixCell CSS class generation in `src/test/java/nl/leonw/competencymatrix/dto/MatrixCellTest.java`
- [ ] T011 [P] Write unit test for MatrixColumnHeader abbreviation generation in `src/test/java/nl/leonw/competencymatrix/dto/MatrixColumnHeaderTest.java`
- [ ] T012 Extend CompetencyService with `buildMatrixViewModel()` method stub in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - View Complete Matrix Overview (Priority: P1) 🎯 MVP

**Goal**: Display all skills as rows and all roles as columns with proficiency levels in cells

**Independent Test**: Navigate to /matrix and verify all skills are alphabetically sorted rows, roles are grouped by family in columns, and proficiency levels appear in cells

### Tests for User Story 1

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T013 [P] [US1] Write integration test for matrix page rendering in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T014 [P] [US1] Write integration test for skill alphabetical sorting in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T015 [P] [US1] Write integration test for role grouping by family in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T016 [P] [US1] Write unit test for role grouping logic in `src/test/java/nl/leonw/competencymatrix/service/CompetencyServiceTest.java`
- [ ] T017 [P] [US1] Write unit test for empty cell creation in `src/test/java/nl/leonw/competencymatrix/service/CompetencyServiceTest.java`

### Implementation for User Story 1

- [ ] T018 [US1] Implement `buildMatrixViewModel()` core logic in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T019 [US1] Implement role grouping and sorting in `buildMatrixViewModel()` in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T020 [US1] Implement MatrixRow construction with empty cell handling in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T021 [US1] Create MatrixOverviewResource with GET /matrix endpoint in `src/main/java/nl/leonw/competencymatrix/resource/MatrixOverviewResource.java`
- [ ] T022 [US1] Create main matrix template in `src/main/resources/templates/matrix-overview.html`
- [ ] T023 [US1] Create matrix CSS styles for table layout in `src/main/resources/META-INF/resources/css/matrix.css`
- [ ] T024 [US1] Implement sticky header CSS (position: sticky) in `src/main/resources/META-INF/resources/css/matrix.css`
- [ ] T025 [US1] Implement level badge CSS styling in `src/main/resources/META-INF/resources/css/matrix.css`
- [ ] T026 [US1] Add matrix navigation link to layout template in `src/main/resources/templates/layout.html`
- [ ] T027 [US1] Include matrix.css in layout template in `src/main/resources/templates/layout.html`

**Checkpoint**: At this point, User Story 1 should be fully functional - users can view complete matrix with sticky headers

---

## Phase 4: User Story 2 - View Proficiency Details on Hover (Priority: P2)

**Goal**: Display tooltip with skill description when hovering over proficiency badge

**Independent Test**: Hover over any proficiency badge and verify tooltip appears with skill name, level, and description

### Tests for User Story 2

- [ ] T028 [P] [US2] Write integration test for tooltip endpoint GET /matrix/tooltips/skill/{id} in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T029 [P] [US2] Write E2E test for hover tooltip display in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T030 [P] [US2] Write E2E test for touch tooltip interaction in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T031 [P] [US2] Write E2E test for tooltip viewport boundaries in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`

### Implementation for User Story 2

- [ ] T032 [US2] Add GET /matrix/tooltips/skill/{skillId} endpoint to MatrixOverviewResource in `src/main/java/nl/leonw/competencymatrix/resource/MatrixOverviewResource.java`
- [ ] T033 [US2] Extend CompetencyService with `getSkillById()` method in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T034 [US2] Add `getDescriptionForLevel()` method to Skill model in `src/main/java/nl/leonw/competencymatrix/model/Skill.java`
- [ ] T035 [US2] Create tooltip fragment template in `src/main/resources/templates/fragments/matrix-tooltip.html`
- [ ] T036 [US2] Add popover attributes to proficiency badges in matrix template in `src/main/resources/templates/matrix-overview.html`
- [ ] T037 [US2] Add htmx lazy loading attributes to proficiency badges in `src/main/resources/templates/matrix-overview.html`
- [ ] T038 [US2] Create matrix.js for tooltip positioning logic in `src/main/resources/META-INF/resources/js/matrix.js`
- [ ] T039 [US2] Implement Popover API event listeners in `src/main/resources/META-INF/resources/js/matrix.js`
- [ ] T040 [US2] Implement viewport boundary detection for tooltips in `src/main/resources/META-INF/resources/js/matrix.js`
- [ ] T041 [US2] Add tooltip CSS styling (max-width 90vw, max-height 90vh) in `src/main/resources/META-INF/resources/css/matrix.css`
- [ ] T042 [US2] Include matrix.js in layout template in `src/main/resources/templates/layout.html`

**Checkpoint**: At this point, User Stories 1 AND 2 should both work - users can view matrix and see tooltip details on hover

---

## Phase 5: User Story 3 - Navigate to Detailed Views (Priority: P3)

**Goal**: Enable navigation to role and skill detail pages by clicking names in matrix

**Independent Test**: Click on role name in column header and verify navigation to role detail page; click on skill name in row header and verify navigation to skill detail page

### Tests for User Story 3

- [ ] T043 [P] [US3] Write E2E test for role name click navigation in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T044 [P] [US3] Write E2E test for skill name click navigation in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T045 [P] [US3] Write E2E test for browser back button from detail view in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`

### Implementation for User Story 3

- [ ] T046 [P] [US3] Add clickable links to role column headers in `src/main/resources/templates/matrix-overview.html`
- [ ] T047 [P] [US3] Add clickable links to skill row headers in `src/main/resources/templates/matrix-overview.html`
- [ ] T048 [US3] Add hover styling for clickable role/skill names in `src/main/resources/META-INF/resources/css/matrix.css`

**Checkpoint**: All three user stories should now be independently functional - matrix view, tooltips, and navigation all working

---

## Phase 6: User Story 4 - Filter and Sort Matrix (Priority: P4)

**Goal**: Allow users to filter skills by category using single-select dropdown

**Independent Test**: Select a category from dropdown and verify only skills in that category are displayed; select "All categories" and verify full matrix returns

### Tests for User Story 4

- [ ] T049 [P] [US4] Write integration test for category filtering in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T050 [P] [US4] Write unit test for filtered MatrixViewModel creation in `src/test/java/nl/leonw/competencymatrix/service/CompetencyServiceTest.java`
- [ ] T051 [P] [US4] Write E2E test for category dropdown interaction in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T052 [P] [US4] Write E2E test for filter clear (All categories) in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`

### Implementation for User Story 4

- [ ] T053 [US4] Update `buildMatrixViewModel()` to support category filtering in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T054 [US4] Add category query parameter handling to GET /matrix endpoint in `src/main/java/nl/leonw/competencymatrix/resource/MatrixOverviewResource.java`
- [ ] T055 [US4] Add category dropdown filter UI to matrix template in `src/main/resources/templates/matrix-overview.html`
- [ ] T056 [US4] Add htmx attributes to category dropdown for partial page updates in `src/main/resources/templates/matrix-overview.html`
- [ ] T057 [US4] Add filter control CSS styling in `src/main/resources/META-INF/resources/css/matrix.css`

**Checkpoint**: All four user stories complete - full matrix functionality with filtering

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Performance optimization, documentation, and final validation

- [ ] T058 [P] Add database indexes for `role_family` and `seniority_order` columns via migration `src/main/resources/db/migration/V3__add_matrix_indexes.sql`
- [ ] T059 [P] Implement caching for unfiltered MatrixViewModel in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T060 [P] Add comprehensive JavaDoc to all DTOs in `src/main/java/nl/leonw/competencymatrix/dto/`
- [ ] T061 [P] Add comprehensive JavaDoc to matrix-related service methods in `src/main/java/nl/leonw/competencymatrix/service/CompetencyService.java`
- [ ] T062 [P] Write E2E test for sticky header scroll behavior in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T063 [P] Write E2E test for horizontal scroll with fixed skill column in `src/test/java/nl/leonw/competencymatrix/e2e/MatrixOverviewE2ETest.java`
- [ ] T064 [P] Write performance test to verify <2 second page load for 100 skills × 20 roles in `src/test/java/nl/leonw/competencymatrix/resource/MatrixOverviewResourceTest.java`
- [ ] T065 Validate quickstart.md instructions by following setup steps
- [ ] T066 Code review: Verify Constitution compliance (Simplicity First, TDD, Data Integrity)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - User stories CAN proceed in parallel if staffed
  - Or sequentially in priority order (US1 → US2 → US3 → US4)
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Builds on US1 matrix display but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Integrates with US1 but independently testable
- **User Story 4 (P4)**: Can start after Foundational (Phase 2) - Filters US1 matrix but independently testable

### Within Each User Story

- Tests MUST be written FIRST and FAIL before implementation (TDD)
- DTOs before service methods
- Service methods before resource endpoints
- Resource endpoints before templates
- Templates before CSS/JS enhancements
- Story complete before moving to next priority

### Parallel Opportunities

- **Phase 1**: T003 (seed data) and T004 (Role model) can run in parallel
- **Phase 2**: All DTO creation (T005-T008) can run in parallel; all unit tests (T009-T011) can run in parallel
- **Phase 3 (US1)**: All tests (T013-T017) can run in parallel; T023-T025 (CSS files) can run in parallel
- **Phase 4 (US2)**: All tests (T028-T031) can run in parallel; T038-T040 (JS implementation) can run in parallel
- **Phase 5 (US3)**: All tests (T043-T045) can run in parallel; T046-T047 (template updates) can run in parallel
- **Phase 6 (US4)**: All tests (T049-T052) can run in parallel
- **Phase 7**: All tasks marked [P] (T058-T064) can run in parallel
- **User Stories**: Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (FIRST - ensure they FAIL):
Task T013: "Write integration test for matrix page rendering"
Task T014: "Write integration test for skill alphabetical sorting"
Task T015: "Write integration test for role grouping by family"
Task T016: "Write unit test for role grouping logic"
Task T017: "Write unit test for empty cell creation"

# After tests fail, launch CSS styling tasks together:
Task T023: "Create matrix CSS styles for table layout"
Task T024: "Implement sticky header CSS"
Task T025: "Implement level badge CSS styling"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (database migration + seed data)
2. Complete Phase 2: Foundational (DTOs + service stubs) - CRITICAL
3. Complete Phase 3: User Story 1 (matrix display)
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo basic matrix view

**At this checkpoint, users can view the complete matrix with sticky headers - delivering core value**

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP! ✅)
3. Add User Story 2 → Test independently → Deploy/Demo (tooltips added)
4. Add User Story 3 → Test independently → Deploy/Demo (navigation added)
5. Add User Story 4 → Test independently → Deploy/Demo (filtering added)
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup (Phase 1) + Foundational (Phase 2) together
2. Once Foundational is done:
   - Developer A: User Story 1 (matrix display)
   - Developer B: User Story 2 (tooltips)
   - Developer C: User Story 3 (navigation)
   - Developer D: User Story 4 (filtering)
3. Stories complete and integrate independently

**NOTE**: User Story 2-4 can be parallelized because they operate on different files and have minimal conflicts. Each story enhances the matrix without modifying others' core functionality.

---

## Task Validation Summary

**Total Tasks**: 66
- **Phase 1 (Setup)**: 4 tasks
- **Phase 2 (Foundational)**: 8 tasks
- **Phase 3 (US1)**: 15 tasks (5 tests + 10 implementation)
- **Phase 4 (US2)**: 15 tasks (4 tests + 11 implementation)
- **Phase 5 (US3)**: 6 tasks (3 tests + 3 implementation)
- **Phase 6 (US4)**: 9 tasks (4 tests + 5 implementation)
- **Phase 7 (Polish)**: 9 tasks

**Test Tasks**: 21 (TDD: tests written before implementation)
**Parallelizable Tasks**: 28 marked with [P]
**Story-Tagged Tasks**: 50 (mapped to US1, US2, US3, US4)

**Independent Test Criteria**:
- **US1**: Navigate to /matrix → see alphabetically sorted skills as rows, role families as column groups
- **US2**: Hover proficiency badge → tooltip appears with description
- **US3**: Click role/skill name → navigate to detail page
- **US4**: Select category filter → matrix shows only filtered skills

**Suggested MVP Scope**: Phase 1 + Phase 2 + Phase 3 (US1 only) = 27 tasks

**Format Validation**: ✅ All tasks follow `- [ ] [ID] [P?] [Story?] Description with file path` format

---

## Notes

- **[P] tasks** = different files, no dependencies - can run in parallel
- **[Story] label** maps task to specific user story for traceability
- **Each user story** is independently completable and testable
- **TDD workflow**: Write tests FIRST (ensure they FAIL), then implement to make them PASS
- **Commit strategy**: Commit after each task or logical group
- **Validation points**: Stop at any checkpoint to validate story independently
- **Avoid**: Vague tasks, same-file conflicts, cross-story dependencies that break independence
