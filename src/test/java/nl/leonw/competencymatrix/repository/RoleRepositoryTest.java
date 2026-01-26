package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class RoleRepositoryTest {

    @Inject
    RoleRepository roleRepository;

    @Test
    void shouldFindSeededRoles() {
        Optional<Role> juniorDev = roleRepository.findByName("Junior Developer");
        assertThat(juniorDev).isPresent();
        assertThat(juniorDev.get().id()).isNotNull();
        assertThat(juniorDev.get().description()).isNotBlank();
    }

    @Test
    void shouldFindByName() {
        Optional<Role> found = roleRepository.findByName("Senior Developer");

        assertThat(found).isPresent();
        assertThat(found.get().name()).isEqualTo("Senior Developer");
    }

    @Test
    void shouldFindAllOrderByName() {
        List<Role> roles = roleRepository.findAllOrderByName();

        assertThat(roles).isNotEmpty();
        assertThat(roles).anyMatch(r -> r.name().equals("Junior Developer"));
        assertThat(roles).anyMatch(r -> r.name().equals("Senior Developer"));
    }
}
