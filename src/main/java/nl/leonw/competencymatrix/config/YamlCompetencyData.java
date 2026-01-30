package nl.leonw.competencymatrix.config;

import java.util.List;
import java.util.Map;

/**
 * In-memory representation of parsed competencies.yaml file.
 * Used during synchronization processing only - not persisted to database.
 */
public record YamlCompetencyData(
    List<CategoryData> categories,
    List<RoleData> roles,
    List<ProgressionData> progressions
) {
    /**
     * Represents a competency category from YAML.
     */
    public record CategoryData(
        String name,
        int displayOrder,
        List<SkillData> skills
    ) {}

    /**
     * Represents a skill from YAML.
     */
    public record SkillData(
        String name,
        String categoryName,
        Map<String, String> levels  // Keys: basic, decent, good, excellent
    ) {}

    /**
     * Represents a role from YAML.
     * UPDATED: Added roleFamily and seniorityOrder for matrix grouping (Feature 004).
     */
    public record RoleData(
        String name,
        String description,
        String roleFamily,
        Integer seniorityOrder,
        List<RequirementData> requirements
    ) {}

    /**
     * Represents a role skill requirement from YAML.
     */
    public record RequirementData(
        String skillName,
        String categoryName,
        String level  // Values: BASIS, REDELIJK, GOED, UITSTEKEND
    ) {}

    /**
     * Represents a role progression from YAML.
     */
    public record ProgressionData(
        String fromRoleName,
        String toRoleName
    ) {}
}
