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
        Role role = new Role(null, "TestSeniorDeveloperExact", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("TestSeniorDeveloperExact");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSeniorDeveloperExact", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_lowercase() {
        // Given
        Role role = new Role(null, "TestSeniorDeveloperLower", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("testseniordeveloperlower");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSeniorDeveloperLower", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_uppercase() {
        // Given
        Role role = new Role(null, "TestSeniorDeveloperUpper", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("TESTSENIORDEVELOPERUPPER");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSeniorDeveloperUpper", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_withExtraSpaces() {
        // Given
        Role role = new Role(null, "TestSeniorDeveloperSpaces", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("  TestSeniorDeveloperSpaces  ");

        // Then
        assertTrue(result.isPresent());
        assertEquals("TestSeniorDeveloperSpaces", result.get().name());
    }

    @Test
    @Transactional
    void testFindByNameIgnoreCase_notFound() {
        // Given
        Role role = new Role(null, "TestSeniorDeveloperMissing", "Experienced developer");
        roleRepository.save(role);

        // When
        Optional<Role> result = roleRepository.findByNameIgnoreCase("MissingDeveloperRole");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    @Transactional
    void testDeleteAll() {
        // Given
        roleRepository.save(new Role(null, "DeleteRoleOne", "Desc"));
        roleRepository.save(new Role(null, "DeleteRoleTwo", "Desc"));

        // When
        int deleted = roleRepository.deleteAll();

        // Then
        assertTrue(deleted >= 2);
        assertTrue(roleRepository.findByNameIgnoreCase("DeleteRoleOne").isEmpty());
        assertTrue(roleRepository.findByNameIgnoreCase("DeleteRoleTwo").isEmpty());
    }
}
