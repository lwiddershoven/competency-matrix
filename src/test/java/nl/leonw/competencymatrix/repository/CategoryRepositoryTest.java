package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CategoryRepositoryTest {

    @Inject
    CategoryRepository categoryRepository;

    @Test
    void shouldFindSeededCategories() {
        Optional<CompetencyCategory> programming = categoryRepository.findByName("Programming");
        assertThat(programming).isPresent();
        assertThat(programming.get().id()).isNotNull();
    }

    @Test
    void shouldFindByName() {
        Optional<CompetencyCategory> found = categoryRepository.findByName("Programming");
        assertThat(found).isPresent();
    }

    @Test
    void shouldFindAllOrderByDisplayOrder() {
        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        assertThat(categories).isNotEmpty();
        assertThat(categories.getFirst().displayOrder())
                .isLessThanOrEqualTo(categories.get(categories.size() - 1).displayOrder());
    }
}
