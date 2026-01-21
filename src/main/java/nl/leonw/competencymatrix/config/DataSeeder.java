package nl.leonw.competencymatrix.config;

import nl.leonw.competencymatrix.model.*;
import nl.leonw.competencymatrix.repository.*;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.RoleProgression;
import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.repository.RoleProgressionRepository;
import nl.leonw.competencymatrix.repository.RoleRepository;
import nl.leonw.competencymatrix.repository.RoleSkillRequirementRepository;
import nl.leonw.competencymatrix.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@DependsOnDatabaseInitialization
@ConditionalOnProperty(
    prefix = "app.data",
    name = "seed",
    havingValue = "true",
    matchIfMissing = true
)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final SkillRepository skillRepository;
    private final RoleSkillRequirementRepository requirementRepository;
    private final RoleProgressionRepository progressionRepository;

    public DataSeeder(CategoryRepository categoryRepository,
                      RoleRepository roleRepository,
                      SkillRepository skillRepository,
                      RoleSkillRequirementRepository requirementRepository,
                      RoleProgressionRepository progressionRepository) {
        this.categoryRepository = categoryRepository;
        this.roleRepository = roleRepository;
        this.skillRepository = skillRepository;
        this.requirementRepository = requirementRepository;
        this.progressionRepository = progressionRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (categoryRepository.count() > 0) {
            log.info("Database already seeded, skipping");
            return;
        }

        log.info("Seeding database from competencies.yaml");
        seedFromYaml();
        log.info("Database seeding complete");
    }

    @SuppressWarnings("unchecked")
    private void seedFromYaml() throws Exception {
        Yaml yaml = new Yaml();
        ClassPathResource resource = new ClassPathResource("seed/competencies.yaml");

        try (InputStream inputStream = resource.getInputStream()) {
            Map<String, Object> data = yaml.load(inputStream);

            // Seed categories
            Map<String, CompetencyCategory> categoryMap = new HashMap<>();
            List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get("categories");
            int displayOrder = 0;
            for (Map<String, Object> catData : categories) {
                String name = (String) catData.get("name");
                CompetencyCategory category = categoryRepository.save(
                        new CompetencyCategory(name, displayOrder++)
                );
                categoryMap.put(name, category);
                log.debug("Created category: {}", name);

                // Seed skills for this category
                List<Map<String, Object>> skills = (List<Map<String, Object>>) catData.get("skills");
                if (skills != null) {
                    for (Map<String, Object> skillData : skills) {
                        Map<String, String> levels = (Map<String, String>) skillData.get("levels");
                        Skill skill = skillRepository.save(new Skill(
                                (String) skillData.get("name"),
                                category.id(),
                                levels != null ? levels.get("basic") : null,
                                levels != null ? levels.get("decent") : null,
                                levels != null ? levels.get("good") : null,
                                levels != null ? levels.get("excellent") : null
                        ));
                        log.debug("Created skill: {} in category {}", skill.name(), name);
                    }
                }
            }

            // Seed roles
            Map<String, Role> roleMap = new HashMap<>();
            List<Map<String, Object>> roles = (List<Map<String, Object>>) data.get("roles");
            for (Map<String, Object> roleData : roles) {
                String name = (String) roleData.get("name");
                String description = (String) roleData.get("description");
                Role role = roleRepository.save(new Role(name, description));
                roleMap.put(name, role);
                log.debug("Created role: {}", name);

                // Seed role skill requirements
                List<Map<String, Object>> requirements = (List<Map<String, Object>>) roleData.get("requirements");
                if (requirements != null) {
                    for (Map<String, Object> reqData : requirements) {
                        String skillName = (String) reqData.get("skill");
                        String categoryName = (String) reqData.get("category");
                        String level = (String) reqData.get("level");

                        CompetencyCategory category = categoryMap.get(categoryName);
                        if (category == null) {
                            log.warn("Category not found: {}", categoryName);
                            continue;
                        }

                        skillRepository.findByNameAndCategoryId(skillName, category.id())
                                .ifPresentOrElse(
                                        skill -> {
                                            requirementRepository.save(new RoleSkillRequirement(
                                                    role.id(), skill.id(), level.toUpperCase()
                                            ));
                                            log.debug("Added requirement: {} -> {} at {}", name, skillName, level);
                                        },
                                        () -> log.warn("Skill not found: {} in category {}", skillName, categoryName)
                                );
                    }
                }
            }

            // Seed role progressions
            List<Map<String, Object>> progressions = (List<Map<String, Object>>) data.get("progressions");
            if (progressions != null) {
                for (Map<String, Object> progData : progressions) {
                    String fromRoleName = (String) progData.get("from");
                    String toRoleName = (String) progData.get("to");

                    Role fromRole = roleMap.get(fromRoleName);
                    Role toRole = roleMap.get(toRoleName);

                    if (fromRole != null && toRole != null) {
                        progressionRepository.save(new RoleProgression(fromRole.id(), toRole.id()));
                        log.debug("Created progression: {} -> {}", fromRoleName, toRoleName);
                    } else {
                        log.warn("Could not create progression from {} to {}", fromRoleName, toRoleName);
                    }
                }
            }
        }
    }
}
