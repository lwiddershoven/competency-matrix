package nl.leonw.competencymatrix.integration;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.config.CompetencySyncService;
import nl.leonw.competencymatrix.config.SyncResult;
import nl.leonw.competencymatrix.config.YamlCompetencyData;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for competency synchronization functionality.
 * Tests end-to-end scenarios with real database operations.
 */
@QuarkusTest
class CompetencySyncIntegrationTest {

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
    @Transactional
    void replaceMode_replacesExistingDataCompletely() {
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "ToBeDeleted", 1));
        Skill skill = skillRepository.save(new Skill(null, "OldSkill", category.id(), "Basic", "Decent", "Good", "Excellent"));
        Role role = roleRepository.save(new Role(null, "OldRole", "Old description"));

        SyncResult result = syncService.syncReplace(createTestYamlData());

        assertTrue(result.categoriesDeleted() >= 1);
        assertTrue(result.skillsDeleted() >= 1);
        assertTrue(result.rolesDeleted() >= 1);
        assertTrue(result.categoriesAdded() >= 1);
        assertTrue(result.skillsAdded() >= 1);
        assertTrue(result.rolesAdded() >= 1);

        assertTrue(categoryRepository.findByNameIgnoreCase("ToBeDeleted").isEmpty());
        assertTrue(skillRepository.findByNameAndCategoryIdIgnoreCase("OldSkill", category.id()).isEmpty());
        assertTrue(roleRepository.findByNameIgnoreCase("OldRole").isEmpty());
        assertFalse(categoryRepository.findByNameIgnoreCase("Programming").isEmpty());
        assertFalse(roleRepository.findByNameIgnoreCase("Senior Developer").isEmpty());
    }

    @Test
    @Transactional
    void replaceMode_removesRolesNotInYaml() {
        Role extraRole = roleRepository.save(new Role(null, "ExtraRole", "Should be deleted"));
        Role extraRole2 = roleRepository.save(new Role(null, "ExtraRole2", "Should also be deleted"));

        SyncResult result = syncService.syncReplace(createTestYamlData());

        assertTrue(result.rolesDeleted() >= 2);
        assertTrue(roleRepository.findByNameIgnoreCase("ExtraRole").isEmpty());
        assertTrue(roleRepository.findByNameIgnoreCase("ExtraRole2").isEmpty());
        assertFalse(roleRepository.findByNameIgnoreCase("Senior Developer").isEmpty());
    }

    @Test
    @Transactional
    void replaceMode_seedsFreshDatabaseWhenEmpty() {
        SyncResult result = syncService.syncReplace(createTestYamlData());

        assertTrue(result.categoriesAdded() >= 1);
        assertTrue(result.skillsAdded() >= 1);
        assertTrue(result.rolesAdded() >= 1);
        assertEquals(0, result.categoriesUpdated());
        assertEquals(0, result.skillsUpdated());
        assertEquals(0, result.rolesUpdated());
        assertEquals(0, result.categoriesDeleted());
        assertEquals(0, result.skillsDeleted());
        assertEquals(0, result.rolesDeleted());

        assertFalse(categoryRepository.findByNameIgnoreCase("Programming").isEmpty());
        assertFalse(roleRepository.findByNameIgnoreCase("Senior Developer").isEmpty());
    }

    private YamlCompetencyData createTestYamlData() {
        YamlCompetencyData.SkillData yamlSkill = new YamlCompetencyData.SkillData(
                "Java",
                "Programming",
                Map.of("basic", "Can write basic Java code", "decent", "Writes clean Java code", "good", "Develops maintainable systems", "excellent", "Masters advanced Java patterns")
        );

        YamlCompetencyData.CategoryData yamlCategory = new YamlCompetencyData.CategoryData(
                "Programming",
                1,
                List.of(yamlSkill)
        );

        YamlCompetencyData.RoleData yamlRole = new YamlCompetencyData.RoleData(
                "Senior Developer",
                "Technical leader who guides team decisions",
                List.of(new YamlCompetencyData.RequirementData("Java", "Programming", "good"))
        );

        return new YamlCompetencyData(
                List.of(yamlCategory),
                List.of(yamlRole),
                List.of()
        );
    }
}