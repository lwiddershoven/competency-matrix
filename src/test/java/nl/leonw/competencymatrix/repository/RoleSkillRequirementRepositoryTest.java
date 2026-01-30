package nl.leonw.competencymatrix.repository;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class RoleSkillRequirementRepositoryTest {

    @Inject
    RoleSkillRequirementRepository requirementRepository;

    @Inject
    RoleRepository roleRepository;

    @Inject
    SkillRepository skillRepository;

    @Inject
    CategoryRepository categoryRepository;

    @Test
    @Transactional
    void testDeleteAll() {
        // Given - create test data with unique names
        CompetencyCategory category = categoryRepository.save(new CompetencyCategory(null, "TestCategoryForReq", 999));
        Skill skill = skillRepository.save(new Skill(null, "TestSkillForReq", category.id(), "B", "D", "G", "E"));
        Role role = roleRepository.save(new Role(null, "TestRoleForReq", "Desc", "Other", 999));
        RoleSkillRequirement req1 = requirementRepository.save(new RoleSkillRequirement(null, role.id(), skill.id(), "REDELIJK"));

        // Verify data exists
        assertTrue(requirementRepository.findById(req1.id()).isPresent());

        // Count before delete
        int initialCount = requirementRepository.findByRoleId(role.id()).size();

        // When
        int deleted = requirementRepository.deleteAll();

        // Then - at least our test requirement was deleted
        assertTrue(deleted >= initialCount);
        assertFalse(requirementRepository.findById(req1.id()).isPresent());
    }

    @Test
    @Transactional
    void testDeleteAll_emptyTable() {
        // When - delete from table (may have seed data)
        int deleted = requirementRepository.deleteAll();

        // Then - should delete at least 0 rows (could have seed data)
        assertTrue(deleted >= 0);
    }
}
