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
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        Skill skill = new Skill(null, "Java", category.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("Java", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("Java", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_lowercase() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        Skill skill = new Skill(null, "Java", category.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("java", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("Java", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_uppercase() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        Skill skill = new Skill(null, "Java", category.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("JAVA", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("Java", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_withSpaces() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        Skill skill = new Skill(null, "Java", category.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("  Java  ", category.id());

        // Then
        assertTrue(result.isPresent());
        assertEquals("Java", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_wrongCategory() {
        // Given
        CompetencyCategory category1 = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        CompetencyCategory category2 = categoryRepository.save(new CompetencyCategory(null, "Communication", 2));
        Skill skill = new Skill(null, "Java", category1.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("Java", category2.id());

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testFindByNameAndCategoryIdIgnoreCase_notFound() {
        // Given
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "Programming", 1));
        Skill skill = new Skill(null, "Java", category.id(), "Basic", "Decent", "Good", "Excellent");
        skillRepository.save(skill);

        // When
        Optional<Skill> result = skillRepository.findByNameAndCategoryIdIgnoreCase("Python", category.id());

        // Then
        assertFalse(result.isPresent());
    }
}
