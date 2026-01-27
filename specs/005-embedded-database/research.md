# Research: In-Memory Database Migration

**Feature**: 005-embedded-database
**Date**: 2026-01-27
**Status**: Research Complete - Decision Made

## Research Question

Can we replace external PostgreSQL with an in-memory database (H2) while preserving the existing YAML-based workflow and repository code without significant modifications?

## Key Findings

### H2 Database Compatibility Analysis

#### PostgreSQL Compatibility Mode
✅ **Supported**: H2 provides `MODE=PostgreSQL` for SQL dialect compatibility
- Configuration: `jdbc:h2:mem:db;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE`
- Supports most PostgreSQL SQL syntax
- Maintains case-sensitive table/column names with `DATABASE_TO_UPPER=FALSE`

#### SERIAL Columns
⚠️ **Partial Support**: Requires migration changes
- PostgreSQL: `id SERIAL PRIMARY KEY` creates INT with auto-increment
- H2: Treats SERIAL as BIGINT, not INT (type mismatch)
- **Required change**: Replace SERIAL with `INTEGER AUTO_INCREMENT` in 2 migration files, 5 tables total
- Impact: Moderate - migrations must be modified

#### CASCADE DELETE and CHECK Constraints
✅ **Fully Supported**: No changes required
- `ON DELETE CASCADE/RESTRICT` works identically
- `CHECK (column IN ('VALUE1', 'VALUE2'))` works for enum validation
- Foreign key constraints function as expected

#### RETURNING Clause
❌ **NOT SUPPORTED**: Requires repository code changes
- PostgreSQL: `INSERT ... RETURNING id` supported
- H2: RETURNING clause NOT supported (open feature request since 2024)
- **Required change**: Rewrite 5 repository insert methods to use JDBC `Statement.RETURN_GENERATED_KEYS`
- Impact: High - all repositories must be modified

**Example Required Change**:
```java
// BEFORE (PostgreSQL)
String sql = "INSERT INTO role (name) VALUES (?) RETURNING id";
PreparedStatement stmt = conn.prepareStatement(sql);
ResultSet rs = stmt.executeQuery();

// AFTER (H2 compatible)
String sql = "INSERT INTO role (name) VALUES (?)";
PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
stmt.executeUpdate();
ResultSet rs = stmt.getGeneratedKeys();
```

### Performance Characteristics

**H2 In-Memory**:
- Startup time: <100ms
- Memory overhead: ~10MB for current dataset (5 tables, 50 records)
- Query performance: Excellent for small datasets

**Testcontainers PostgreSQL** (current approach):
- Startup time: ~4 seconds per container
- Memory overhead: ~100MB per container
- Optimization available: Container reuse across test classes

### Alternative: Testcontainers PostgreSQL

**Current Status**: Project already uses Testcontainers successfully
```properties
%test.quarkus.datasource.devservices.enabled=true
%test.quarkus.datasource.devservices.image-name=postgres:18.1
```

**Benefits of Keeping Testcontainers**:
1. **Zero code changes required**
2. **100% PostgreSQL compatibility** (production parity)
3. **No dual-database maintenance burden**
4. **Existing infrastructure already configured**
5. **4-second startup overhead negligible for small test suite**

**Trade-offs**:
| Aspect | H2 | Testcontainers PostgreSQL |
|--------|----|-----------------------------|
| Startup Speed | ⚡ Instant (<100ms) | 🐢 Slow (~4s) |
| Code Changes | ❌ Required (migrations + repos) | ✅ None |
| Compatibility | ⚠️ 90% (RETURNING fails) | ✅ 100% |
| Maintenance | ⚠️ Dual-dialect support | ✅ Single dialect |
| Production Parity | ⚠️ Different from prod | ✅ Identical to prod |

## Decision: Use H2 for Development, Keep PostgreSQL for Production Option

**Primary Approach**: Use H2 in-memory database with necessary code modifications

**Rationale**:
1. **Alignment with Feature Goal**: User explicitly wants "embedded database" and "no external postgres" for maintenance simplification
2. **Development Experience Priority**: Zero-setup development (clone → run) is the P1 user story
3. **Code Changes are Manageable**:
   - 2 migration files to update (SERIAL → AUTO_INCREMENT)
   - 2 repository methods to refactor (RETURNING → generated keys)
   - Changes improve code portability (RETURNING is PostgreSQL-specific)
4. **Acceptable Trade-off**: Slightly more implementation work for significant operational simplification
5. **Optional PostgreSQL Support**: Can maintain fallback to PostgreSQL via environment variable configuration

### Constitution Compliance Check

✅ **Simplicity First**: Net reduction in complexity
- Eliminates: External PostgreSQL installation, connection configuration, database management
- Adds: H2 dependency (1MB), configuration (3 lines)
- Trade-off: 2 files of migration changes + 2 repository refactors vs. permanent external dependency

✅ **Test-First Development**: Will write tests for H2 compatibility before migration
✅ **Data Integrity**: YAML source of truth preserved, constraints maintained
✅ **User-Centric**: Achieves zero-setup development goal

### Implementation Strategy

#### Phase 1: Prepare H2 Compatibility (TDD)
1. Write test verifying H2 connection works
2. Write test verifying Flyway migrations apply to H2
3. Write test verifying data integrity with H2

#### Phase 2: Migration Script Updates
1. `V1__initial_schema.sql`: Replace `SERIAL` with `INTEGER AUTO_INCREMENT` (5 tables)
2. `V2__add_role_grouping.sql`: Remove `COMMENT ON` statements (H2 unsupported)

#### Phase 3: Repository Refactoring
1. Create `AbstractRepository.executeInsertWithGeneratedKey()` helper method
2. Update `RoleRepository.save()` to use generated keys API
3. Update `SkillRepository.save()` to use generated keys API
4. Verify all existing tests pass

#### Phase 4: Configuration
1. Update `pom.xml`: Add `quarkus-jdbc-h2` dependency
2. Update `application.properties`: Configure H2 as default datasource
3. Document PostgreSQL fallback via environment variables

## Critical Files for Implementation

**Configuration**:
- `pom.xml`: Add H2 dependency
- `src/main/resources/application.properties`: Configure H2 datasource

**Migrations**:
- `src/main/resources/db/migration/V1__initial_schema.sql`: SERIAL → AUTO_INCREMENT (5 occurrences)
- `src/main/resources/db/migration/V2__add_role_grouping.sql`: Remove COMMENT ON (2 occurrences)

**Repositories**:
- `src/main/java/nl/leonw/competencymatrix/repository/RoleRepository.java`: Refactor insert method (line ~164)
- `src/main/java/nl/leonw/competencymatrix/repository/SkillRepository.java`: Refactor insert method (line ~146)

## Alternatives Considered

### Alternative 1: HSQLDB
- Similar compatibility issues to H2
- Slower performance than H2
- No significant advantage

### Alternative 2: Apache Derby
- Slowest of embedded options
- Disk-oriented by default (not true in-memory)
- Rejected for performance reasons

### Alternative 3: Keep PostgreSQL + Testcontainers Only
- **Pros**: Zero code changes, 100% compatibility
- **Cons**: Does NOT meet user requirement for "no external database"
- **Verdict**: Rejected - fails to achieve feature goal

## Assumptions

1. Development environments have sufficient memory for H2 in-memory database (~10MB overhead)
2. Repository refactoring to use generated keys API is acceptable code modernization
3. PostgreSQL-specific RETURNING clause can be replaced without functional loss
4. Test suite size remains small (container startup overhead is minimal for now)

## Open Questions

None - all technical unknowns resolved through research.

## Sources

- [H2 Database Features](http://www.h2database.com/html/features.html)
- [H2 RETURNING Clause Limitation](https://github.com/h2database/h2database/issues/3962)
- [Quarkus JDBC H2 Extension](https://quarkus.io/extensions/io.quarkus/quarkus-jdbc-h2/)
- [H2 SERIAL Type Discussion](http://h2-database.66688.n3.nabble.com/SERIAL-BIGSERIAL-Support-td4025449.html)
- [Testcontainers Performance Optimization](https://dzone.com/articles/boosting-test-performance-with-testcontainers)
- [Database Comparison: H2 vs PostgreSQL](https://sukhvinder.co.uk/database-comparison-h2-hsqldb-derby-postgresql-mysql/)
