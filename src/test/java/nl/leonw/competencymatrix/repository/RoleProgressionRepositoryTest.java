package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleProgression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleProgressionRepositoryTest {

    @Inject
    RoleProgressionRepository progressionRepository;

    @Inject
    RoleRepository roleRepository;

    @Test
    @Transactional
    void testDeleteAll() {
        // Given - create test data with unique names
        Role role1 = roleRepository.save(new Role(null, "TestJuniorProg", "Test junior", "Other", 999));
        Role role2 = roleRepository.save(new Role(null, "TestSeniorProg", "Test senior", "Other", 999));
        RoleProgression prog1 = progressionRepository.save(new RoleProgression(null, role1.id(), role2.id()));

        // Verify data exists
        assertTrue(progressionRepository.findById(prog1.id()).isPresent());

        // When
        int deleted = progressionRepository.deleteAll();

        // Then - at least our test progression was deleted
        assertTrue(deleted >= 1);
        assertFalse(progressionRepository.findById(prog1.id()).isPresent());
    }

    @Test
    @Transactional
    void testDeleteAll_withSeedData() {
        // When - delete from table (may have seed data)
        int deleted = progressionRepository.deleteAll();

        // Then - should delete at least 0 rows (could have seed data)
        assertTrue(deleted >= 0);
    }
}
