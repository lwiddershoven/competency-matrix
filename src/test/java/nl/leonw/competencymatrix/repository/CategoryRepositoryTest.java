package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class CategoryRepositoryTest {

    @Inject
    CategoryRepository categoryRepository;

    @Test
    @Transactional
    void testFindByNameIgnoreCase_exactMatch() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategoryExact", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("Programming");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Programming", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_lowercase() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategoryLower", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("testcategorylower");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestCategoryLower", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_uppercase() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategoryUpper", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("TESTCATEGORYUPPER");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestCategoryUpper", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_mixedCase() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategoryMixed", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("TeStCaTeGoRyMiXeD");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestCategoryMixed", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_withExtraSpaces() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategorySpaces", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("  TestCategorySpaces  ");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestCategorySpaces", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_notFound() {
        // Given
        CompetencyCategory category = new CompetencyCategory(null, "TestCategoryNotFound", 1);
        categoryRepository.save(category);

        // When
        Optional<CompetencyCategory> result = categoryRepository.findByNameIgnoreCase("NonExistent");

        // Then
        assertFalse(result.isPresent());
    }
}
