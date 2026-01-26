# Quickstart Guide: Competencies Data Synchronization

**Feature**: 003-competencies-sync
**Audience**: Developers implementing or testing this feature
**Prerequisites**: Java 25, Maven, Docker (for Testcontainers)

## Overview

This guide helps developers set up their environment to work on the competencies data synchronization feature. It covers configuration, testing, and common development workflows.

## Configuration

### Application Property

Add or modify the sync mode property in `src/main/resources/application.properties`:

```properties
# Competency sync mode: none, merge, or replace
competency.sync.mode=merge
```

**Values**:
- `none`: Disable sync (default if property missing)
- `merge`: Incremental updates (add new, update existing, keep rest)
- `replace`: Full replacement (delete all, seed fresh)

### Test Configuration

Override sync mode in test profile at `src/test/resources/application-test.properties`:

```properties
# Disable sync for most tests (enable per-test as needed)
competency.sync.mode=none
```

Or override programmatically in test classes:

```java
@Test
@TestProfile(MergeSyncProfile.class)
void testMergeSync() {
    // Test merge behavior
}

public static class MergeSyncProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("competency.sync.mode", "merge");
    }
}
```

## Development Workflow

### 1. Modify competencies.yaml

Edit `src/main/resources/seed/competencies.yaml`:

```yaml
categories:
  - name: "Programming"
    skills:
      - name: "Java"
        levels:
          basic: "Can write basic Java code"
          decent: "Writes clean code with OOP principles"
          good: "Designs maintainable systems"
          excellent: "Masters advanced patterns"
      # Add new skill here
      - name: "Kotlin"
        levels:
          basic: "Understands Kotlin syntax"
          decent: "Uses Kotlin idioms effectively"
          good: "Builds production systems"
          excellent: "Contributes to Kotlin ecosystem"
```

### 2. Set Sync Mode

For **merge** (incremental update):
```properties
competency.sync.mode=merge
```

For **replace** (full reset):
```properties
competency.sync.mode=replace
```

### 3. Run Application

```bash
# Start with PostgreSQL via Testcontainers
./mvnw quarkus:dev

# Or with existing PostgreSQL
./mvnw quarkus:dev -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/competency_matrix
```

### 4. Verify Sync

Check application logs for sync summary:

```
INFO  [DataSeeder] Starting competency sync - mode: merge
INFO  [DataSeeder] Category synced: Programming
INFO  [DataSeeder] Skill synced: Kotlin in category Programming
INFO  [DataSeeder] Sync complete: 1 categories updated, 1 skills added, 0 roles updated
```

### 5. Query Database

Verify changes persisted:

```bash
# Connect to dev database
psql -h localhost -U competency_user -d competency_matrix

# Check for new skill
SELECT s.name, c.name as category
FROM skills s
JOIN competency_categories c ON s.category_id = c.id
WHERE s.name = 'Kotlin';
```

## Testing

### Unit Tests

Test sync logic in isolation:

```java
@QuarkusTest
class CompetencySyncServiceTest {

    @Inject
    CompetencySyncService syncService;

    @Test
    void testMergeAddsNewSkill() {
        // Arrange: Prepare YAML data with new skill
        YamlCompetencyData data = // ... parse test YAML

        // Act: Sync in merge mode
        SyncResult result = syncService.syncMerge(data);

        // Assert: Verify skill added
        assertEquals(1, result.skillsAdded());
    }

    @Test
    void testCaseInsensitiveMatching() {
        // Arrange: DB has "Java", YAML has "java"
        // Act: Sync
        // Assert: No duplicate created, existing skill updated
    }
}
```

### Integration Tests

Test full sync with database:

```java
@QuarkusTest
@TestProfile(TestcontainersProfile.class)
class CompetencySyncIntegrationTest {

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    SkillRepository skillRepository;

    @Test
    @Transactional
    void testMergeSyncWithDatabase() {
        // Arrange: Seed initial data
        CompetencyCategory category = categoryRepository.save(
            new CompetencyCategory("Programming", 0)
        );

        // Act: Run sync with modified YAML
        // (Use test resource file: src/test/resources/test-competencies.yaml)

        // Assert: Verify database state
        List<Skill> skills = skillRepository.findByCategory(category.id());
        assertTrue(skills.stream().anyMatch(s -> s.name().equals("Kotlin")));
    }

    @Test
    void testReplaceModeDeletesExisting() {
        // Arrange: Seed data
        // Act: Sync with replace mode
        // Assert: Old data gone, new data present
    }
}
```

### Test Data Files

Create test YAML files in `src/test/resources/`:

**test-competencies-merge.yaml**:
```yaml
categories:
  - name: "Programming"
    skills:
      - name: "Java"
        levels:
          basic: "Updated description"
          decent: "Updated description"
          good: "Updated description"
          excellent: "Updated description"
```

**test-competencies-replace.yaml**:
```yaml
categories:
  - name: "Testing"
    skills:
      - name: "JUnit"
        levels:
          basic: "Writes basic tests"
          decent: "Uses mocking effectively"
          good: "Practices TDD"
          excellent: "Defines testing strategies"
```

### Edge Case Tests

```java
@QuarkusTest
class SyncValidationTest {

    @Test
    void testInvalidYamlSyntaxFailsStartup() {
        // Arrange: Point to malformed YAML
        // Act & Assert: Expect RuntimeException
        assertThrows(RuntimeException.class, () -> {
            // Trigger sync with invalid YAML
        });
    }

    @Test
    void testMissingSkillReferenceFailsStartup() {
        // Arrange: Role requirement references non-existent skill
        // Act & Assert: Expect RuntimeException with clear message
    }

    @Test
    void testInvalidSyncModeFailsStartup() {
        // Arrange: Set competency.sync.mode=invalid
        // Act & Assert: Expect RuntimeException listing valid values
    }
}
```

## Debugging

### Enable Debug Logging

Add to `application.properties`:

```properties
# Verbose sync logging
quarkus.log.category."nl.leonw.competencymatrix.config".level=DEBUG
```

### Inspect Transaction Rollback

When sync fails, verify no partial changes:

```bash
# Before sync attempt
psql> SELECT COUNT(*) FROM skills;
 count
-------
    50

# After failed sync (should be unchanged)
psql> SELECT COUNT(*) FROM skills;
 count
-------
    50
```

### View Startup Logs

Sync happens during application startup. Check logs for:

```
INFO  [DataSeeder] Starting competency sync - mode: merge
ERROR [DataSeeder] Sync failed: Role 'Architect' references skill 'Leadership' which does not exist in database or YAML
ERROR [io.quarkus.runtime.Application] Failed to start application
```

## Common Scenarios

### Scenario 1: Add New Skill to Existing Category

1. Edit `competencies.yaml`: Add skill under existing category
2. Set `competency.sync.mode=merge`
3. Restart app
4. Verify: New skill added, existing data unchanged

**Expected Log**:
```
INFO  [DataSeeder] Skill synced: NewSkillName in category Programming
INFO  [DataSeeder] Sync complete: 0 categories updated, 1 skills added
```

### Scenario 2: Update Skill Level Descriptions

1. Edit `competencies.yaml`: Modify level descriptions
2. Set `competency.sync.mode=merge`
3. Restart app
4. Verify: Skill descriptions updated

**Expected Log**:
```
INFO  [DataSeeder] Skill synced: Java in category Programming
INFO  [DataSeeder] Sync complete: 0 categories updated, 1 skills updated
```

### Scenario 3: Full Database Reset

1. Edit `competencies.yaml`: Make significant changes
2. Set `competency.sync.mode=replace`
3. Restart app
4. Verify: Database matches YAML exactly

**Expected Log**:
```
INFO  [DataSeeder] Starting competency sync - mode: replace
INFO  [DataSeeder] Deleted existing data: 50 skills, 5 categories, 3 roles
INFO  [DataSeeder] Sync complete: 3 categories added, 45 skills added, 2 roles added
```

### Scenario 4: Disable Sync in Production

1. Set `competency.sync.mode=none`
2. Restart app
3. Verify: No database changes, regardless of YAML content

**Expected Log**:
```
INFO  [DataSeeder] Competency sync disabled (mode: none)
```

## Performance Testing

### Measure Sync Duration

```java
@QuarkusTest
class SyncPerformanceTest {

    @Test
    void testSyncCompletesWithinTimeLimit() {
        // Arrange: Large YAML with 100 skills + 20 roles
        YamlCompetencyData data = loadLargeTestData();

        // Act: Measure sync time
        long startTime = System.currentTimeMillis();
        syncService.syncMerge(data);
        long duration = System.currentTimeMillis() - startTime;

        // Assert: Meets 5-second requirement
        assertTrue(duration < 5000, "Sync took " + duration + "ms, expected <5000ms");
    }
}
```

### Profile with JFR

```bash
# Run with Java Flight Recorder
./mvnw quarkus:dev \
  -Djava.flight.recorder.options=dumponexit=true,filename=sync-profile.jfr

# Analyze with jmc or visualvm
jmc sync-profile.jfr
```

## Troubleshooting

### Issue: Sync Not Running

**Symptom**: No sync logs on startup

**Cause**: `competency.sync.mode=none` (default if property missing)

**Solution**: Set `competency.sync.mode=merge` or `replace` in `application.properties`

### Issue: Duplicate Entities Created

**Symptom**: Multiple skills with similar names (e.g., "Java" and "java")

**Cause**: Case-sensitive matching in old code path

**Solution**: Verify using new `findByNameIgnoreCase` methods in repositories

### Issue: Sync Fails with "Skill Not Found"

**Symptom**: `ERROR Role 'X' references skill 'Y' which does not exist`

**Cause**: Role requirement references skill not in YAML or database

**Solution**: Add skill to YAML under appropriate category, or fix requirement reference

### Issue: Transaction Rollback

**Symptom**: No changes persisted after sync attempt

**Cause**: Any error during sync triggers rollback (by design)

**Solution**: Check logs for specific error, fix YAML/data issue, retry

### Issue: Slow Sync Performance

**Symptom**: Sync takes >5 seconds

**Cause**: Inefficient queries or large dataset

**Solution**:
1. Verify database indexes exist on name columns
2. Check for N+1 query patterns
3. Profile with JFR to identify bottlenecks

## Next Steps

After setting up development environment:

1. **Read**: [data-model.md](./data-model.md) for detailed entity relationships
2. **Review**: [research.md](./research.md) for technical decisions
3. **Implement**: Follow tasks in [tasks.md](./tasks.md) (generated by `/speckit.tasks`)
4. **Test**: Write tests first (TDD per Constitution)

## Reference Commands

```bash
# Build project
./mvnw clean package

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CompetencySyncServiceTest

# Run with dev mode (hot reload)
./mvnw quarkus:dev

# Run with specific sync mode
./mvnw quarkus:dev -Dcompetency.sync.mode=merge

# View application logs
tail -f target/quarkus.log

# Connect to dev database
psql -h localhost -U competency_user -d competency_matrix

# Reset database (testcontainers auto-creates fresh DB on restart)
./mvnw clean quarkus:dev
```

## Additional Resources

- **Quarkus Config Guide**: https://quarkus.io/guides/config-reference
- **Quarkus Testing Guide**: https://quarkus.io/guides/getting-started-testing
- **Testcontainers Documentation**: https://www.testcontainers.org/
- **Project Constitution**: `.specify/memory/constitution.md`
