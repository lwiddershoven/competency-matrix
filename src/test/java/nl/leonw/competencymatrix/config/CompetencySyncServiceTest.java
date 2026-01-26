package nl.leonw.competencymatrix.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleProgression;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleProgressionRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CompetencySyncServiceTest {

    @Inject
    CompetencySyncService syncService;

    @Inject
    CategoryRepository categoryRepository;

    @Inject
    SkillRepository skillRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    RoleSkillRequirementRepository requirementRepository;

    @Inject
    RoleProgressionRepository progressionRepository;

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

    @Test
    @Transactional
    void syncMerge_updatesExistingCategory() {
        CompetencyCategory existing = categoryRepository.save(new CompetencyCategory(null, "MergeCategory", 1));

        YamlCompetencyData data = new YamlCompetencyData(
                List.of(new YamlCompetencyData.CategoryData("MergeCategory", 5, List.of())),
                List.of(),
                List.of()
        );

        SyncResult result = syncService.syncMerge(data);

        CompetencyCategory updated = categoryRepository.findByNameIgnoreCase("MergeCategory").orElseThrow();
        assertEquals(existing.id(), updated.id());
        assertEquals(5, updated.displayOrder());
        assertEquals(1, result.categoriesUpdated());
    }

    @Test
    @Transactional
    void syncMerge_updatesExistingSkill() {
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "SkillCategory", 1));
        skillRepository.save(new Skill(null, "MergeSkill", category.id(), "Old", "Old", "Old", "Old"));

        YamlCompetencyData.SkillData yamlSkill = new YamlCompetencyData.SkillData(
                "MergeSkill",
                "SkillCategory",
                Map.of("basic", "New", "decent", "New", "good", "New", "excellent", "New")
        );
        YamlCompetencyData data = new YamlCompetencyData(
                List.of(new YamlCompetencyData.CategoryData("SkillCategory", 1, List.of(yamlSkill))),
                List.of(),
                List.of()
        );

        SyncResult result = syncService.syncMerge(data);

        Skill updated = skillRepository.findByNameAndCategoryIdIgnoreCase("MergeSkill", category.id()).orElseThrow();
        assertEquals("New", updated.goodDescription());
        assertEquals(1, result.skillsUpdated());
    }

    @Test
    @Transactional
    void syncMerge_updatesExistingRole() {
        Role existing = roleRepository.save(new Role(null, "MergeRole", "Old description"));

        YamlCompetencyData.RoleData yamlRole = new YamlCompetencyData.RoleData(
                "MergeRole",
                "New description",
                List.of()
        );
        YamlCompetencyData data = new YamlCompetencyData(
                List.of(),
                List.of(yamlRole),
                List.of()
        );

        SyncResult result = syncService.syncMerge(data);

        Role updated = roleRepository.findByNameIgnoreCase("MergeRole").orElseThrow();
        assertEquals(existing.id(), updated.id());
        assertEquals("New description", updated.description());
        assertEquals(1, result.rolesUpdated());
    }

    @Test
    @Transactional
    void syncReplace_deletesEntitiesInDependencyOrder() {
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "ReplaceCategory", 1));
        Skill skill = skillRepository.save(new Skill(null, "ReplaceSkill", category.id(), "B", "D", "G", "E"));
        Role role = roleRepository.save(new Role(null, "ReplaceRole", "Desc"));
        requirementRepository.save(new RoleSkillRequirement(null, role.id(), skill.id(), "GOOD"));
        Role toRole = roleRepository.save(new Role(null, "ReplaceRoleNext", "Desc"));
        progressionRepository.save(new RoleProgression(null, role.id(), toRole.id()));

        SyncResult result = syncService.syncReplace(new YamlCompetencyData(List.of(), List.of(), List.of()));

        assertTrue(result.progressionsDeleted() >= 1);
        assertTrue(result.requirementsDeleted() >= 1);
        assertTrue(result.skillsDeleted() >= 1);
        assertTrue(result.categoriesDeleted() >= 1);
        assertTrue(result.rolesDeleted() >= 1);
        assertTrue(categoryRepository.findByNameIgnoreCase("ReplaceCategory").isEmpty());
        assertTrue(skillRepository.findByNameAndCategoryIdIgnoreCase("ReplaceSkill", category.id()).isEmpty());
        assertTrue(roleRepository.findByNameIgnoreCase("ReplaceRole").isEmpty());
        assertTrue(roleRepository.findByNameIgnoreCase("ReplaceRoleNext").isEmpty());
    }
}
