package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.Skill;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SkillRepositoryTest {

    @Inject
    SkillRepository skillRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_exactMatch() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryExact", 1));
        Skill skill = new Skill(null, "TestSkillExact", category.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("TestSkillExact", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSkillExact", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_lowercase() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryLower", 1));
        Skill skill = new Skill(null, "TestSkillLower", category.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("testskilllower", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSkillLower", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_uppercase() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryUpper", 1));
        Skill skill = new Skill(null, "TestSkillUpper", category.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("TESTSKILLUPPER", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSkillUpper", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_withSpaces() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategorySpaces", 1));
        Skill skill = new Skill(null, "TestSkillSpaces", category.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("  TestSkillSpaces  ", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSkillSpaces", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_wrongCategory() {
        // Given
        CompetencyCategory category1 = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryOne", 1));
        CompetencyCategory category2 = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryTwo", 2));
        Skill skill = new Skill(null, "TestSkillWrongCategory", category1.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("TestSkillWrongCategory", category2.id());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_notFound() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestSkillCategoryMissing", 1));
        Skill skill = new Skill(null, "TestSkillMissing", category.id(), "Basis", "Redelijk", "Goed", "Uitstekend");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("OtherSkillMissing", category.id());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testDeleteAll() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "DeleteSkills", 1));
        skillRepository.save(new Skill(null, "DeleteSkillOne", category.id(), "B", "D", "G", "E"));
        skillRepository.save(new Skill(null, "DeleteSkillTwo", category.id(), "B", "D", "G", "E"));

        // When
        int deleted = skillRepository.deleteAll();

        // Then
        assertTrue(deleted >= 2);
        assertTrue(skillRepository.findByCategoryId(category.id()).isEmpty());
        categoryRepository.deleteAll();
    }
}
