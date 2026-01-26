package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleRepositoryTest {

    @Inject
    RoleRepository roleRepository;

    @Test
    @Transactional
    void testFindByNameIgnoreCase_exactMatch() {
        // Given
        Role role = new Role(null, "Senior Developer", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("Senior Developer");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Senior Developer", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_lowercase() {
        // Given
        Role role = new Role(null, "Senior Developer", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("senior developer");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Senior Developer", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_uppercase() {
        // Given
        Role role = new Role(null, "Senior Developer", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("SENIOR DEVELOPER");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Senior Developer", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_withExtraSpaces() {
        // Given
        Role role = new Role(null, "Senior Developer", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("  Senior Developer  ");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Senior Developer", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_notFound() {
        // Given
        Role role = new Role(null, "Senior Developer", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("Junior Developer");

        // Then
        assertFalse(result.isPresent());
    }
}
