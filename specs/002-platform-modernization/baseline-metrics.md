# Baseline Metrics - Pre-Quarkus Migration

**Date**: 2026-01-21
**Git Tag**: pre-quarkus-migration
**Spring Boot Version**: 4.0.1

## Test Coverage

**Total Tests**: 29
**Pass Rate**: 100% (29/29 passing, 0 failures, 0 errors, 0 skipped)

### Test Breakdown

| Test Class | Tests | Result |
|------------|-------|--------|
| CategoryRepositoryTest | 3 | ✅ PASS |
| RoleRepositoryTest | 3 | ✅ PASS |
| RoleControllerTest | 3 | ✅ PASS |
| CompareControllerTest | 3 | ✅ PASS |
| HomeControllerTest | 2 | ✅ PASS |
| CompetencyMatrixApplicationTests | 1 | ✅ PASS |
| CompetencyServiceTest | 6 | ✅ PASS |
| BrowseCompetenciesTest (E2E) | 8 | ✅ PASS |

**Total Test Execution Time**: ~12 seconds

## Performance Metrics

**Note**: Full startup time measurement not available (requires running database).

## Database Baseline

**Note**: Database not accessible during baseline capture. Row counts will be verified when database is available during actual migration.

Expected tables:
- rolename
- skill
- competency_category
- role_skill_requirement
- role_progression

## Success Criteria for Migration

- ✅ **Test Pass Rate**: Maintain 100% (29/29 tests passing)
- ✅ **Zero Data Loss**: All row counts must match baseline
- ✅ **Schema Unchanged**: Database schema must remain identical
- ✅ **Performance**: Page loads <2 seconds (per SC-003)
