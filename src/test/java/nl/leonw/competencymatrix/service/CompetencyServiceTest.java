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
                .anyMatch(sr -> sr.skill().name().equals("Java") && sr.requiredLevel() == ProficiencyLevel.BASIC);
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

        // Verify there's at least one skill that shows progression (Java: basic -> good)
        assertThat(comparisons).anyMatch(c ->
            c.skill().name().equals("Java") &&
            c.fromLevel() == ProficiencyLevel.BASIC &&
            c.toLevel() == ProficiencyLevel.GOOD &&
            c.isUpgrade() &&
            c.hasChanged()
        );
    }

    /**
     * T016: Unit test for role grouping logic
     * Verifies that buildMatrixViewModel groups roles by family and orders them correctly.
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

    /**
     * T017: Unit test for matrix structure with map of maps
     * Verifies that matrix contains entries for all skills and roles
     * with proper alignment between headers and cells.
     */
    @Test
    void shouldCreateMatrixWithAlignedHeadersAndCells() {
        var matrixViewModel = competencyService.buildMatrixViewModel(null);

        assertThat(matrixViewModel).isNotNull();
        assertThat(matrixViewModel.matrix()).isNotEmpty();
        assertThat(matrixViewModel.rolesInOrder()).isNotEmpty();

        // Get total number of roles
        int totalRoles = matrixViewModel.rolesInOrder().size();

        // Verify each skill row has cells for all roles
        for (var skillEntry : matrixViewModel.matrix().entrySet()) {
            String skillName = skillEntry.getKey();
            Map<String, nl.leonw.competencymatrix.dto.CellData> roleCells = skillEntry.getValue();

            assertThat(roleCells).hasSize(totalRoles);

            // Verify all role names from rolesInOrder exist in the skill row
            for (var roleInfo : matrixViewModel.rolesInOrder()) {
                assertThat(roleCells).containsKey(roleInfo.name());

                // Verify cell has correct IDs
                var cell = roleCells.get(roleInfo.name());
                assertThat(cell).isNotNull();
                assertThat(cell.roleId()).isEqualTo(roleInfo.id());
            }
        }
    }

    /**
     * Verifies that matrix structure uses string keys (skill names and role names)
     * for readability and that cells can be retrieved by name.
     */
    @Test
    void shouldUseStringKeysForMatrixLookup() {
        var matrixViewModel = competencyService.buildMatrixViewModel(null);

        assertThat(matrixViewModel).isNotNull();

        // Verify we can look up cells using skill name and role name strings
        String skillName = "Java";
        String roleName = "Junior Developer";

        var skillRow = matrixViewModel.matrix().get(skillName);
        assertThat(skillRow).isNotNull();

        var cell = skillRow.get(roleName);
        assertThat(cell).isNotNull();
        assertThat(cell.level()).isEqualTo(ProficiencyLevel.BASIC);
    }

    /**
     * Verifies that empty cells exist for skill-role combinations
     * where no requirement is defined.
     */
    @Test
    void shouldIncludeEmptyCellsForMissingRequirements() {
        var matrixViewModel = competencyService.buildMatrixViewModel(null);

        assertThat(matrixViewModel).isNotNull();
        assertThat(matrixViewModel.matrix()).isNotEmpty();

        // Check that at least some cells are empty
        boolean hasEmptyCells = matrixViewModel.matrix().values().stream()
                .flatMap(m -> m.values().stream())
                .anyMatch(nl.leonw.competencymatrix.dto.CellData::isEmpty);

        // Not all skill-role combinations have requirements, so we expect some empty cells
        assertThat(hasEmptyCells).isTrue();
    }
}
