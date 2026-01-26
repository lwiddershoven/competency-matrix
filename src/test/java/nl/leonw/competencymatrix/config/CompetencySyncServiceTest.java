package nl.leonw.competencymatrix.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CompetencySyncServiceTest {

    @Inject
    CompetencySyncService syncService;

    @Test
    void testParseYaml_validStructure() {
        // Given
        String yaml = """
                categories:
                  - name: "Programming"
                    skills:
                      - name: "Java"
                        levels:
                          basic: "Basic Java"
                          decent: "Decent Java"
                          good: "Good Java"
                          excellent: "Excellent Java"
                roles:
                  - name: "Developer"
                    description: "Software developer"
                    requirements:
                      - skill: "Java"
                        category: "Programming"
                        level: "decent"
                progressions:
                  - from: "Junior"
                    to: "Senior"
                """;

        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        // When
        YamlCompetencyData result = syncService.parseYaml(inputStream);

        // Then
        assertNotNull(result);
        assertEquals(1, result.categories().size());
        assertEquals("Programming", result.categories().get(0).name());
        assertEquals(1, result.categories().get(0).skills().size());
        assertEquals("Java", result.categories().get(0).skills().get(0).name());

        assertEquals(1, result.roles().size());
        assertEquals("Developer", result.roles().get(0).name());

        assertEquals(1, result.progressions().size());
        assertEquals("Junior", result.progressions().get(0).fromRoleName());
        assertEquals("Senior", result.progressions().get(0).toRoleName());
    }

    @Test
    void testParseYaml_malformedYaml() {
        // Given - invalid YAML syntax
        String yaml = "categories:\n  - name: Programming\n    skills: [incomplete";
        InputStream inputStream = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8));

        // When & Then
        assertThrows(RuntimeException.class, () -> syncService.parseYaml(inputStream));
    }

    @Test
    void testNormalize() {
        // When & Then
        assertEquals("programming", syncService.normalize("Programming"));
        assertEquals("programming", syncService.normalize("PROGRAMMING"));
        assertEquals("programming", syncService.normalize("  Programming  "));
        assertEquals("programming", syncService.normalize("programming"));
        assertEquals("senior developer", syncService.normalize("Senior  Developer"));
        assertEquals("", syncService.normalize(null));
        assertEquals("", syncService.normalize(""));
    }

    @Test
    void testValidateYaml_validStructure() {
        // Given
        YamlCompetencyData.CategoryData category = new YamlCompetencyData.CategoryData(
                "Programming", 1,
                java.util.List.of(new YamlCompetencyData.SkillData("Java", "Programming",
                        java.util.Map.of("basic", "B", "decent", "D", "good", "G", "excellent", "E")))
        );
        YamlCompetencyData.RoleData role = new YamlCompetencyData.RoleData(
                "Developer", "Desc", java.util.List.of()
        );
        YamlCompetencyData data = new YamlCompetencyData(
                java.util.List.of(category),
                java.util.List.of(role),
                java.util.List.of()
        );

        // When & Then - should not throw
        assertDoesNotThrow(() -> syncService.validateYaml(data));
    }

    @Test
    void testValidateYaml_missingCategoryName() {
        // Given - category without name
        YamlCompetencyData.CategoryData category = new YamlCompetencyData.CategoryData(
                null, 1, java.util.List.of()
        );
        YamlCompetencyData data = new YamlCompetencyData(
                java.util.List.of(category),
                java.util.List.of(),
                java.util.List.of()
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> syncService.validateYaml(data));
        assertTrue(exception.getMessage().contains("Category name is required"));
    }

    @Test
    void testValidateYaml_missingSkillLevels() {
        // Given - skill without all required levels
        YamlCompetencyData.SkillData skill = new YamlCompetencyData.SkillData(
                "Java", "Programming", java.util.Map.of("basic", "B")  // Missing decent, good, excellent
        );
        YamlCompetencyData.CategoryData category = new YamlCompetencyData.CategoryData(
                "Programming", 1, java.util.List.of(skill)
        );
        YamlCompetencyData data = new YamlCompetencyData(
                java.util.List.of(category),
                java.util.List.of(),
                java.util.List.of()
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> syncService.validateYaml(data));
        assertTrue(exception.getMessage().contains("must have all four levels"));
    }
}
