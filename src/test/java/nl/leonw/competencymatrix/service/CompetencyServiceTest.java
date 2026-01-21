package nl.leonw.competencymatrix.service;

import nl.leonw.competencymatrix.TestcontainersConfiguration;
import nl.leonw.competencymatrix.model.*;
import nl.leonw.competencymatrix.repository.*;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleProgression;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleProgressionRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@Transactional
class CompetencyServiceTest {

    @Autowired
    private CompetencyService competencyService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RoleSkillRequirementRepository requirementRepository;

    @Autowired
    private RoleProgressionRepository progressionRepository;

    private Role juniorRole;
    private Role seniorRole;
    private CompetencyCategory programmingCategory;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        // Clear any seeded data
        requirementRepository.deleteAll();
        progressionRepository.deleteAll();
        skillRepository.deleteAll();
        categoryRepository.deleteAll();
        roleRepository.deleteAll();

        // Create test data
        juniorRole = roleRepository.save(new Role("Junior Developer", "Entry level"));
        seniorRole = roleRepository.save(new Role("Senior Developer", "Senior level"));

        programmingCategory = categoryRepository.save(new CompetencyCategory("Programming", 1));
        javaSkill = skillRepository.save(new Skill(
                "Java", programmingCategory.id(),
                "Basic Java", "Decent Java", "Good Java", "Excellent Java"
        ));

        requirementRepository.save(new RoleSkillRequirement(juniorRole.id(), javaSkill.id(), "BASIC"));
        requirementRepository.save(new RoleSkillRequirement(seniorRole.id(), javaSkill.id(), "GOOD"));

        progressionRepository.save(new RoleProgression(juniorRole.id(), seniorRole.id()));
    }

    @Test
    void shouldGetAllRoles() {
        List<Role> roles = competencyService.getAllRoles();
        assertThat(roles).hasSize(2);
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

        assertThat(result).hasSize(1);
        assertThat(result).containsKey(programmingCategory);

        List<CompetencyService.SkillWithRequirement> skills = result.get(programmingCategory);
        assertThat(skills).hasSize(1);
        assertThat(skills.getFirst().skill().name()).isEqualTo("Java");
        assertThat(skills.getFirst().requiredLevel()).isEqualTo(ProficiencyLevel.BASIC);
    }

    @Test
    void shouldGetNextRoles() {
        List<Role> nextRoles = competencyService.getNextRoles(juniorRole.id());
        assertThat(nextRoles).hasSize(1);
        assertThat(nextRoles.getFirst().name()).isEqualTo("Senior Developer");
    }

    @Test
    void shouldGetPreviousRoles() {
        List<Role> prevRoles = competencyService.getPreviousRoles(seniorRole.id());
        assertThat(prevRoles).hasSize(1);
        assertThat(prevRoles.getFirst().name()).isEqualTo("Junior Developer");
    }

    @Test
    void shouldCompareRoles() {
        List<CompetencyService.SkillComparison> comparisons =
                competencyService.compareRoles(juniorRole.id(), seniorRole.id());

        assertThat(comparisons).hasSize(1);

        CompetencyService.SkillComparison comparison = comparisons.getFirst();
        assertThat(comparison.skill().name()).isEqualTo("Java");
        assertThat(comparison.fromLevel()).isEqualTo(ProficiencyLevel.BASIC);
        assertThat(comparison.toLevel()).isEqualTo(ProficiencyLevel.GOOD);
        assertThat(comparison.isUpgrade()).isTrue();
        assertThat(comparison.hasChanged()).isTrue();
    }
}
