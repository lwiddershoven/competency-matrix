package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.TestcontainersConfiguration;
import nl.leonw.competencymatrix.model.Role;
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
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldSaveAndFindRole() {
        Role role = new Role("Test Role", "A test role description");
        Role saved = roleRepository.save(role);

        assertThat(saved.id()).isNotNull();

        Optional<Role> found = roleRepository.findById(saved.id());
        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Test Role");
        assertThat(found.get().description()).isEqualTo("A test role description");
    }

    @Test
    void shouldFindByName() {
        Role role = roleRepository.save(new Role("Unique Role", "Description"));

        Optional<Role> found = roleRepository.findByName("Unique Role");

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(role.id());
    }

    @Test
    void shouldFindAllOrderByName() {
        roleRepository.save(new Role("Zebra Role", "Last"));
        roleRepository.save(new Role("Alpha Role", "First"));

        List<Role> roles = roleRepository.findAllOrderByName();

        assertThat(roles).extracting(Role::name)
                .containsSequence("Alpha Role", "Zebra Role");
    }
}
