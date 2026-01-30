package nl.leonw.competencymatrix.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * Matrix for looking up proficiency levels by role and skill name.
 * Provides explicit get(roleName, skillName) lookup that returns empty string when no requirement exists.
 */
public class CompetencyMatrix {
    private final Map<String, Map<String, String>> data; // roleName -> (skillName -> level)

    /**
     * Create matrix from role-skill level mappings.
     * @param data Map of roleName to (skillName to level display text)
     */
    public CompetencyMatrix(Map<String, Map<String, String>> data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        // Deep copy for immutability
        this.data = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
            this.data.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
    }

    /**
     * Get proficiency level for a role-skill combination.
     * @param roleName Name of the role
     * @param skillName Name of the skill
     * @return Level display text (e.g., "Basis", "Goed") or empty string if no requirement
     */
    public String getLevel(String roleName, String skillName) {
        if (roleName == null || skillName == null) {
            return "";
        }
        return data.getOrDefault(roleName, Map.of()).getOrDefault(skillName, "");
    }

    /**
     * Get CSS class for level badge.
     * @param roleName Name of the role
     * @param skillName Name of the skill
     * @return CSS class (e.g., "level-basis") or empty string if no requirement
     */
    public String getLevelClass(String roleName, String skillName) {
        String level = getLevel(roleName, skillName);
        if (level.isEmpty()) {
            return "";
        }
        // Convert display name to lowercase for CSS class
        return "level-" + level.toLowerCase();
    }

    /**
     * Get uppercase level name for API parameters (e.g., "BASIS" from "Basis").
     * @param roleName Name of the role
     * @param skillName Name of the skill
     * @return Uppercase level name or empty string if no requirement
     */
    public String getLevelName(String roleName, String skillName) {
        String level = getLevel(roleName, skillName);
        if (level.isEmpty()) {
            return "";
        }
        return level.toUpperCase();
    }

    /**
     * Check if a role-skill combination has a requirement.
     * @param roleName Name of the role
     * @param skillName Name of the skill
     * @return true if requirement exists, false otherwise
     */
    public boolean hasRequirement(String roleName, String skillName) {
        if (roleName == null || skillName == null) {
            return false;
        }
        Map<String, String> roleMap = data.get(roleName);
        if (roleMap == null) {
            return false;
        }
        return roleMap.containsKey(skillName);
    }
}
