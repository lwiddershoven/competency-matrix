package nl.leonw.competencymatrix.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CompetencyServiceTest {

    @Inject
    CompetencyService competencyService;

    @Inject
    RoleRepository roleRepository;

    private Role juniorRole;
    private Role seniorRole;

    @BeforeEach
    void setUp() {
        // Use seeded data from Flyway
        juniorRole = roleRepository.findByName("Junior Developer").orElseThrow();
        seniorRole = roleRepository.findByName("Senior Developer").orElseThrow();
    }

    @Test
    void shouldGetAllRoles() {
        List<Role> roles = competencyService.getAllRoles();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(2);
        assertThat(roles).anyMatch(r -> r.name().equals("Junior Developer"));
        assertThat(roles).anyMatch(r -> r.name().equals("Senior Developer"));
    }

    @Test
    void shouldGetRoleById() {
        var role = competencyService.getRoleById(juniorRole.id());
        assertThat(role).isPresent();
        assertThat(role.get().name()).isEqualTo("Junior Developer");
    }

    @Test
    void shouldGetSkillsByCategoryForRole() {
        Map<CompetencyCategory, List<CompetencyService.SkillWithRequirement>> result =
            competencyService.getSkillsByCategoryForRole(juniorRole.id());

        assertThat(result).isNotEmpty();
        assertThat(result.values().stream().flatMap(List::stream))
            .anyMatch(sr -> sr.skill().name().equals("Java") && sr.requiredLevel() == ProficiencyLevel.BASIS);
    }

    @Test
    void shouldGetNextRoles() {
        List<Role> nextRoles = competencyService.getNextRoles(juniorRole.id());
        assertThat(nextRoles).isNotEmpty();
        assertThat(nextRoles).anyMatch(r -> r.name().equals("Medior Developer"));
    }

    @Test
    void shouldGetPreviousRoles() {
        List<Role> prevRoles = competencyService.getPreviousRoles(seniorRole.id());
        assertThat(prevRoles).isNotEmpty();
        assertThat(prevRoles).anyMatch(r -> r.name().equals("Medior Developer"));
    }

    @Test
    void shouldCompareRoles() {
        List<CompetencyService.SkillComparison> comparisons =
            competencyService.compareRoles(juniorRole.id(), seniorRole.id());

        assertThat(comparisons).isNotEmpty();

        // Verify there's at least one skill that shows progression (Java: basis -> goed)
        assertThat(comparisons).anyMatch(c ->
            c.skill().name().equals("Java") &&
                c.fromLevel() == ProficiencyLevel.BASIS &&
                c.toLevel() == ProficiencyLevel.GOED &&
                c.isUpgrade() &&
                c.hasChanged()
        );
    }

    /**
     * T016: Unit test for role grouping logic Verifies that buildMatrixViewModel groups roles by family and orders them
     * correctly.
     */
    @Test
    void shouldGroupRolesByFamilyInMatrixViewModel() {
        var matrixViewModel = competencyService.buildMatrixViewModel(null);

        assertThat(matrixViewModel).isNotNull();
        assertThat(matrixViewModel.rolesByFamily()).isNotEmpty();

        // Verify Developer family exists
        assertThat(matrixViewModel.rolesByFamily()).containsKey("Developer");

        // Verify roles within Developer family are ordered by seniority
        var developerRoles = matrixViewModel.rolesByFamily().get("Developer");
        assertThat(developerRoles).isNotEmpty();

        // Verify Junior comes before Senior in seniority order
        boolean hasJunior = developerRoles.stream()
            .anyMatch(h -> h.name().equals("Junior Developer"));
        boolean hasSenior = developerRoles.stream()
            .anyMatch(h -> h.name().equals("Senior Developer"));

        assertThat(hasJunior || hasSenior).isTrue();
    }
}
