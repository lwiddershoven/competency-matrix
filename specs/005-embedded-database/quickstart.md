# Quickstart: In-Memory Database Migration

**Feature**: 005-embedded-database
**Target Audience**: Developers implementing this feature
**Estimated Time**: 2-4 hours

## Prerequisites

- [ ] Java 25 installed
- [ ] Maven 3.9+ installed
- [ ] IDE with Java support (IntelliJ IDEA, VS Code, Eclipse)
- [ ] Git repository cloned
- [ ] Existing test suite passes with PostgreSQL

## Goal

Migrate from external PostgreSQL to H2 in-memory database while maintaining all functionality. After completion, developers should be able to clone and run the application without installing PostgreSQL.

## Implementation Checklist

### Phase 1: Test-First Setup ✅

Before making any changes, establish baseline:

```bash
# Ensure all existing tests pass with PostgreSQL
mvn clean test

# Note the test execution time for comparison
# Expected: ~10-15 seconds (with Testcontainers PostgreSQL)
```

**Expected Result**: All tests pass, green build

---

### Phase 2: Add H2 Dependency

**File**: `pom.xml`

**Action**: Replace PostgreSQL with H2

```xml
<!-- REMOVE THIS -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>

<!-- ADD THIS -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-jdbc-h2</artifactId>
</dependency>
```

**Verify**:
```bash
mvn clean compile
# Should complete without errors
```

---

### Phase 3: Update Configuration

**File**: `src/main/resources/application.properties`

**Action**: Configure H2 datasource

```properties
# BEFORE
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/competencymatrix
quarkus.datasource.username=competency
quarkus.datasource.password=competency

# AFTER
quarkus.datasource.db-kind=h2
quarkus.datasource.jdbc.url=jdbc:h2:mem:competencymatrix;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE
quarkus.datasource.username=sa
quarkus.datasource.password=
```

**Action**: Update test configuration

```properties
# BEFORE
%test.quarkus.datasource.devservices.enabled=true
%test.quarkus.datasource.devservices.image-name=postgres:18.1
%test.flyway.clean-at-start=true

# AFTER
# Remove test-specific overrides - use same H2 config as dev
%test.flyway.clean-at-start=true
%test.competency.sync.mode=replace
```

**Verify**: Configuration syntax is correct

---

### Phase 4: Update Migration Scripts

#### Migration V1: Replace SERIAL with AUTO_INCREMENT

**File**: `src/main/resources/db/migration/V1__initial_schema.sql`

**Action**: Replace all SERIAL declarations (5 occurrences):

```sql
-- BEFORE
CREATE TABLE competency_category (
    id SERIAL PRIMARY KEY,

-- AFTER
CREATE TABLE competency_category (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
```

**Tables to update**:
1. `competency_category`
2. `rolename`
3. `skill`
4. `role_skill_requirement`
5. `role_progression`

**Tip**: Use find-and-replace: `SERIAL` → `INTEGER AUTO_INCREMENT`

#### Migration V2: Remove COMMENT ON Statements

**File**: `src/main/resources/db/migration/V2__add_role_grouping.sql`

**Action**: Remove or comment out COMMENT ON statements:

```sql
-- BEFORE
COMMENT ON COLUMN rolename.role_family IS 'Groups roles: Developer, Architect, Operations, Other';
COMMENT ON COLUMN rolename.seniority_order IS 'Ordering within family: 1=Junior, 2=Medior, 3=Senior';

-- AFTER
-- H2 does not support COMMENT ON - documentation moved to data-model.md
```

**Verify**: Run Flyway migration test

```bash
mvn clean test -Dtest=FlywayMigrationTest
# Should pass if migrations are syntactically correct
```

---

### Phase 5: Refactor Repositories (TDD Approach)

#### Step 5a: Write Failing Test

**File**: `src/test/java/nl/leonw/competencymatrix/repository/RoleRepositoryTest.java`

**Action**: Add test for H2 compatibility

```java
@Test
void testInsertReturnsGeneratedId() {
    // Given
    Role newRole = new Role(null, "Test Role", "Test description", "Developer", 1);

    // When
    Role savedRole = roleRepository.save(newRole);

    // Then
    assertNotNull(savedRole.id(), "Generated ID should not be null");
    assertTrue(savedRole.id() > 0, "Generated ID should be positive");

    // Verify we can retrieve it
    Optional<Role> retrieved = roleRepository.findById(savedRole.id());
    assertTrue(retrieved.isPresent());
    assertEquals("Test Role", retrieved.get().name());
}
```

**Run**: Test should fail initially (RETURNING clause not supported by H2)

#### Step 5b: Refactor RoleRepository

**File**: `src/main/java/nl/leonw/competencymatrix/repository/RoleRepository.java`

**Find**: `save()` method (around line 164)

**Before**:
```java
public Role save(Role role) {
    String sql = """
        INSERT INTO rolename (name, description, role_family, seniority_order)
        VALUES (?, ?, ?, ?) RETURNING id
        """;

    try (PreparedStatement stmt = dataSource.getConnection().prepareStatement(sql)) {
        stmt.setString(1, role.name());
        stmt.setString(2, role.description());
        stmt.setString(3, role.roleFamily());
        stmt.setInt(4, role.seniorityOrder());

        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return new Role(rs.getInt("id"), role.name(), role.description(),
                               role.roleFamily(), role.seniorityOrder());
            }
        }
    }
    throw new RuntimeException("Failed to insert role");
}
```

**After**:
```java
public Role save(Role role) {
    String sql = """
        INSERT INTO rolename (name, description, role_family, seniority_order)
        VALUES (?, ?, ?, ?)
        """;

    try (Connection conn = dataSource.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

        stmt.setString(1, role.name());
        stmt.setString(2, role.description());
        stmt.setString(3, role.roleFamily());
        stmt.setInt(4, role.seniorityOrder());

        int affectedRows = stmt.executeUpdate();
        if (affectedRows == 0) {
            throw new RuntimeException("Failed to insert role, no rows affected");
        }

        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
            if (generatedKeys.next()) {
                Integer id = generatedKeys.getInt(1);
                return new Role(id, role.name(), role.description(),
                               role.roleFamily(), role.seniorityOrder());
            } else {
                throw new RuntimeException("Failed to insert role, no ID obtained");
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("Failed to save role: " + e.getMessage(), e);
    }
}
```

**Key Changes**:
1. Remove `RETURNING id` from SQL
2. Add `Statement.RETURN_GENERATED_KEYS` to prepareStatement()
3. Change `executeQuery()` to `executeUpdate()`
4. Use `getGeneratedKeys()` to retrieve ID
5. Access ID by column index (1) instead of name ("id")

**Run**: Test should now pass

```bash
mvn test -Dtest=RoleRepositoryTest#testInsertReturnsGeneratedId
```

#### Step 5c: Refactor SkillRepository

**File**: `src/main/java/nl/leonw/competencymatrix/repository/SkillRepository.java`

**Find**: `save()` method (around line 146)

**Action**: Apply the same refactoring pattern as RoleRepository

**Changes**:
1. Remove `RETURNING id` from INSERT statement
2. Use `Statement.RETURN_GENERATED_KEYS`
3. Change `executeQuery()` to `executeUpdate()`
4. Use `getGeneratedKeys()` to retrieve generated ID

**Run**: Test SkillRepository

```bash
mvn test -Dtest=SkillRepositoryTest
```

---

### Phase 6: Verify Full Test Suite

Run complete test suite to ensure no regressions:

```bash
mvn clean test
```

**Expected Results**:
- ✅ All tests pass
- ⏱️ Faster execution (no Testcontainers startup delay)
- 📊 Test time: ~5-10 seconds (improved from ~15 seconds)

**If tests fail**:
1. Check error messages for SQL syntax issues
2. Verify H2 configuration in application.properties
3. Ensure migrations were updated correctly
4. Check repository refactoring for typos

---

### Phase 7: Manual Verification

Start application in dev mode:

```bash
mvn quarkus:dev
```

**Verify**:
1. ✅ Application starts without PostgreSQL running
2. ✅ Startup completes in under 5 seconds
3. ✅ Console shows H2 connection (not PostgreSQL)
4. ✅ Navigate to http://localhost:8080
5. ✅ Competency matrix displays with all data from YAML
6. ✅ Check for errors in console logs

**Expected Console Output**:
```
INFO  [io.quarkus] (Quarkus Main Thread) Installed features: [agroal, cdi, flyway, jdbc-h2, ...]
INFO  [nl.leonw.competencymatrix.config.DataSeeder] Starting competency sync...
INFO  [nl.leonw.competencymatrix.config.CompetencySyncService] Sync mode: REPLACE
INFO  [nl.leonw.competencymatrix.config.CompetencySyncService] Successfully synced 6 categories, 40 skills, 10 roles
```

---

### Phase 8: Data Integrity Check

Verify all data from YAML is present:

**Categories**:
- [ ] Programming
- [ ] Software Design
- [ ] DevOps & Infrastructure
- [ ] Quality & Testing
- [ ] Soft Skills
- [ ] Architecture Frameworks

**Sample Roles**:
- [ ] Junior Developer
- [ ] Medior Developer
- [ ] Senior Developer
- [ ] Lead Developer
- [ ] Solution Architect

**Sample Skills**:
- [ ] Java
- [ ] Spring Framework
- [ ] Git
- [ ] Design Patterns
- [ ] Unit Testing

**Role Requirements**:
- [ ] Junior Developer requires Java (BASIC)
- [ ] Senior Developer requires Design Patterns (GOOD)
- [ ] Lead Developer requires Mentoring (EXCELLENT)

---

### Phase 9: Performance Validation

Measure key performance metrics:

```bash
# Time application startup
time mvn quarkus:dev

# Expected: <5 seconds to "Listening on http://0.0.0.0:8080"
```

**Success Criteria** (from spec):
- [x] SC-002: Application startup < 5 seconds
- [x] SC-003: All tests pass without modification
- [x] SC-004: Test suite < 30 seconds
- [x] SC-007: Page load < 2 seconds

---

### Phase 10: Clean Up

**Optional**: Remove PostgreSQL-related configuration

1. Remove `docker-compose.yml` if only used for development database
2. Update `.gitignore` to remove PostgreSQL-specific entries
3. Update README with new zero-setup instructions

**Git Commit**:
```bash
git add -A
git commit -m "Migrate to H2 in-memory database

- Replace PostgreSQL with H2 for zero-setup development
- Update Flyway migrations (SERIAL → AUTO_INCREMENT)
- Refactor repositories (RETURNING → generated keys API)
- Remove test container configuration
- All tests passing, startup time improved

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

---

## Troubleshooting Guide

### Issue: Flyway Migration Fails

**Error**: `org.flywaydb.core.api.exception.FlywayValidateException`

**Solution**:
- Check V1__initial_schema.sql for remaining SERIAL keywords
- Verify V2 migration has no COMMENT ON statements
- Clear any existing H2 database files (should be in memory only)

### Issue: Generated ID is NULL

**Error**: `NullPointerException when accessing saved entity ID`

**Solution**:
- Ensure `Statement.RETURN_GENERATED_KEYS` is passed to prepareStatement()
- Verify you're calling `executeUpdate()`, not `executeQuery()`
- Check that you're reading from `getGeneratedKeys()`, not the main ResultSet

### Issue: Table Not Found

**Error**: `Table "COMPETENCY_CATEGORY" not found`

**Solution**:
- Check H2 URL includes `DATABASE_TO_UPPER=FALSE`
- Verify Flyway migrations are applied at startup
- Check `quarkus.flyway.migrate-at-start=true` is set

### Issue: Slow Startup

**Symptom**: Startup takes > 5 seconds

**Solution**:
- H2 in-memory should be near-instant; check for network calls
- Verify not accidentally connecting to external PostgreSQL
- Check logs for errors or warnings during sync

### Issue: Data Missing After Restart

**Expected Behavior**: This is normal! H2 is in-memory, data reloads from YAML each startup

**Verify**: Check that `competencies.yaml` contains all expected data

---

## Success Checklist

### Development Experience
- [ ] Clone repository → run application in under 2 minutes
- [ ] No PostgreSQL installation required
- [ ] Application starts in under 5 seconds
- [ ] All competency data visible in UI

### Code Quality
- [ ] All tests pass (green build)
- [ ] Test execution under 30 seconds
- [ ] No deprecation warnings
- [ ] Code follows existing patterns

### Data Integrity
- [ ] All YAML data loaded correctly
- [ ] Foreign key constraints working
- [ ] Cascade deletes functioning
- [ ] Sync modes (REPLACE, MERGE, NONE) all work

### Documentation
- [ ] README updated with new setup instructions
- [ ] Migration changes documented
- [ ] Known limitations noted (if any)

---

## Next Steps

After successful migration:

1. **Update Documentation**:
   - Update project README
   - Document zero-setup development workflow
   - Note PostgreSQL is no longer required

2. **Run Full CI Pipeline**:
   - Verify CI builds pass
   - Check deployment to Clever Cloud works
   - Validate no environment-specific issues

3. **Consider Cleanup**:
   - Remove unused PostgreSQL configuration files
   - Archive old database backups
   - Update onboarding documentation

4. **Monitor**:
   - Watch for memory usage in production
   - Verify startup times remain consistent
   - Check for any H2-specific issues

---

## Estimated Timeline

| Phase | Task | Time |
|-------|------|------|
| 1 | Test-first setup | 15 min |
| 2 | Add H2 dependency | 5 min |
| 3 | Update configuration | 10 min |
| 4 | Update migrations | 20 min |
| 5 | Refactor repositories | 60 min |
| 6 | Run test suite | 10 min |
| 7 | Manual verification | 20 min |
| 8 | Data integrity check | 15 min |
| 9 | Performance validation | 10 min |
| 10 | Clean up | 15 min |
| **Total** | | **~3 hours** |

Add buffer time for troubleshooting and learning curve.

---

## Resources

- [H2 Database Documentation](http://www.h2database.com/html/main.html)
- [Quarkus Datasources Guide](https://quarkus.io/guides/datasource)
- [Flyway Migration Guide](https://flywaydb.org/documentation/concepts/migrations)
- [JDBC Generated Keys Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/basics/retrieving.html)
- [Feature Specification](spec.md)
- [Data Model Documentation](data-model.md)
- [Research Findings](research.md)
