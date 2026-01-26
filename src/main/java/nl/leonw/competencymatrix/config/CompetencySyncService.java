package nl.leonw.competencymatrix.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core service for synchronizing competencies from YAML to database.
 * Handles parsing, validation, and normalization operations.
 */
@ApplicationScoped
public class CompetencySyncService {

    private static final Logger log = LoggerFactory.getLogger(CompetencySyncService.class);

    /**
     * Parses competencies.yaml file into in-memory data structures.
     *
     * @param inputStream YAML file input stream
     * @return Parsed competency data
     * @throws RuntimeException if YAML parsing fails
     */
    @SuppressWarnings("unchecked")
    public YamlCompetencyData parseYaml(InputStream inputStream) {
        try {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(inputStream);

            List<YamlCompetencyData.CategoryData> categories = parseCategories(
                    (List<Map<String, Object>>) data.get("categories"));
            List<YamlCompetencyData.RoleData> roles = parseRoles(
                    (List<Map<String, Object>>) data.get("roles"));
            List<YamlCompetencyData.ProgressionData> progressions = parseProgressions(
                    (List<Map<String, Object>>) data.get("progressions"));

            return new YamlCompetencyData(categories, roles, progressions);
        } catch (Exception e) {
            log.error("Failed to parse YAML file", e);
            throw new RuntimeException("Failed to parse competencies.yaml: " + e.getMessage(), e);
        }
    }

    /**
     * Validates the structure and content of parsed YAML data.
     *
     * @param data Parsed YAML data to validate
     * @throws RuntimeException if validation fails
     */
    public void validateYaml(YamlCompetencyData data) {
        if (data.categories() == null || data.roles() == null || data.progressions() == null) {
            throw new RuntimeException("Invalid YAML: missing required top-level keys (categories, roles, progressions)");
        }

        // Validate categories
        for (YamlCompetencyData.CategoryData category : data.categories()) {
            if (category.name() == null || category.name().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Category name is required");
            }
            if (category.skills() == null) {
                throw new RuntimeException("Invalid YAML: Category '" + category.name() + "' is missing 'skills' list");
            }

            // Validate skills
            for (YamlCompetencyData.SkillData skill : category.skills()) {
                if (skill.name() == null || skill.name().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Skill name is required in category '" + category.name() + "'");
                }
                if (skill.levels() == null || skill.levels().size() != 4) {
                    throw new RuntimeException("Invalid YAML: Skill '" + skill.name() + "' must have all four levels (basic, decent, good, excellent)");
                }
                if (!skill.levels().containsKey("basic") || !skill.levels().containsKey("decent") ||
                    !skill.levels().containsKey("good") || !skill.levels().containsKey("excellent")) {
                    throw new RuntimeException("Invalid YAML: Skill '" + skill.name() + "' must have levels: basic, decent, good, excellent");
                }
            }
        }

        // Validate roles
        for (YamlCompetencyData.RoleData role : data.roles()) {
            if (role.name() == null || role.name().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Role name is required");
            }
            if (role.description() == null) {
                throw new RuntimeException("Invalid YAML: Role '" + role.name() + "' is missing 'description'");
            }
            if (role.requirements() == null) {
                throw new RuntimeException("Invalid YAML: Role '" + role.name() + "' is missing 'requirements' list");
            }

            // Validate requirements
            for (YamlCompetencyData.RequirementData req : role.requirements()) {
                if (req.skillName() == null || req.skillName().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing skill name");
                }
                if (req.categoryName() == null || req.categoryName().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing category name");
                }
                if (req.level() == null || req.level().trim().isEmpty()) {
                    throw new RuntimeException("Invalid YAML: Requirement in role '" + role.name() + "' missing level");
                }
            }
        }

        // Validate progressions
        for (YamlCompetencyData.ProgressionData progression : data.progressions()) {
            if (progression.fromRoleName() == null || progression.fromRoleName().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Progression missing 'from' role name");
            }
            if (progression.toRoleName() == null || progression.toRoleName().trim().isEmpty()) {
                throw new RuntimeException("Invalid YAML: Progression missing 'to' role name");
            }
        }

        log.debug("YAML validation passed");
    }

    /**
     * Normalizes a string for case/space-insensitive matching.
     * Converts to lowercase, trims, and collapses multiple spaces.
     *
     * @param value String to normalize
     * @return Normalized string (lowercase, trimmed, single spaces)
     */
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.CategoryData> parseCategories(List<Map<String, Object>> categoriesList) {
        if (categoriesList == null) {
            return List.of();
        }

        List<YamlCompetencyData.CategoryData> categories = new ArrayList<>();
        int displayOrder = 0;

        for (Map<String, Object> categoryMap : categoriesList) {
            String name = (String) categoryMap.get("name");
            List<Map<String, Object>> skillsList = (List<Map<String, Object>>) categoryMap.get("skills");

            List<YamlCompetencyData.SkillData> skills = new ArrayList<>();
            if (skillsList != null) {
                for (Map<String, Object> skillMap : skillsList) {
                    String skillName = (String) skillMap.get("name");
                    Map<String, String> levels = (Map<String, String>) skillMap.get("levels");

                    skills.add(new YamlCompetencyData.SkillData(skillName, name, levels));
                }
            }

            categories.add(new YamlCompetencyData.CategoryData(name, displayOrder++, skills));
        }

        return categories;
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.RoleData> parseRoles(List<Map<String, Object>> rolesList) {
        if (rolesList == null) {
            return List.of();
        }

        List<YamlCompetencyData.RoleData> roles = new ArrayList<>();

        for (Map<String, Object> roleMap : rolesList) {
            String name = (String) roleMap.get("name");
            String description = (String) roleMap.get("description");
            List<Map<String, Object>> requirementsList = (List<Map<String, Object>>) roleMap.get("requirements");

            List<YamlCompetencyData.RequirementData> requirements = new ArrayList<>();
            if (requirementsList != null) {
                for (Map<String, Object> reqMap : requirementsList) {
                    String skillName = (String) reqMap.get("skill");
                    String categoryName = (String) reqMap.get("category");
                    String level = (String) reqMap.get("level");

                    requirements.add(new YamlCompetencyData.RequirementData(skillName, categoryName, level));
                }
            }

            roles.add(new YamlCompetencyData.RoleData(name, description, requirements));
        }

        return roles;
    }

    @SuppressWarnings("unchecked")
    private List<YamlCompetencyData.ProgressionData> parseProgressions(List<Map<String, Object>> progressionsList) {
        if (progressionsList == null) {
            return List.of();
        }

        List<YamlCompetencyData.ProgressionData> progressions = new ArrayList<>();

        for (Map<String, Object> progressionMap : progressionsList) {
            String from = (String) progressionMap.get("from");
            String to = (String) progressionMap.get("to");

            progressions.add(new YamlCompetencyData.ProgressionData(from, to));
        }

        return progressions;
    }
}
