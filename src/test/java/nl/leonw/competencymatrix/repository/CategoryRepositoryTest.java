package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.TestcontainersConfiguration;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void shouldSaveAndFindCategory() {
        CompetencyCategory category = new CompetencyCategory("Programming", 1);
        CompetencyCategory saved = categoryRepository.save(category);

        assertThat(saved.id()).isNotNull();

        Optional<CompetencyCategory> found = categoryRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Programming");
        assertThat(found.get().displayOrder()).isEqualTo(1);
    }

    @Test
    void shouldFindByName() {
        categoryRepository.save(new CompetencyCategory("Unique Category", 0));

        Optional<CompetencyCategory> found = categoryRepository.findByName("Unique Category");

        assertThat(found).isPresent();
    }

    @Test
    void shouldFindAllOrderByDisplayOrder() {
        categoryRepository.save(new CompetencyCategory("Second", 2));
        categoryRepository.save(new CompetencyCategory("First", 1));
        categoryRepository.save(new CompetencyCategory("Third", 3));

        List<CompetencyCategory> categories = categoryRepository.findAllOrderByDisplayOrder();

        assertThat(categories).extracting(CompetencyCategory::displayOrder)
                .containsSequence(1, 2, 3);
    }
}
