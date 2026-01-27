package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.ProficiencyLevel;

/**
 * A single cell in the matrix showing proficiency requirement for a skill-role combination.
 *
 * Feature: 004-matrix-overview
 * Purpose: Represents matrix cell data with empty cell support for missing requirements
 */
public record MatrixCell(
    Long skillId,
    Integer roleId,
    ProficiencyLevel requiredLevel,
    boolean isEmpty
) {
    /**
     * Create cell with proficiency requirement.
     *
     * @param skillId ID of the skill
     * @param roleId ID of the role
     * @param level Required proficiency level
     * @return MatrixCell with level data
     */
    public static MatrixCell withLevel(Long skillId, Integer roleId, ProficiencyLevel level) {
        if (skillId == null || roleId == null || level == null) {
            throw new IllegalArgumentException("skillId, roleId, and level must not be null for withLevel");
        }
        return new MatrixCell(skillId, roleId, level, false);
    }

    /**
     * Create empty cell (no proficiency requirement for this skill-role combination).
     *
     * @param skillId ID of the skill
     * @param roleId ID of the role
     * @return MatrixCell marked as empty
     */
    public static MatrixCell empty(Long skillId, Integer roleId) {
        if (skillId == null || roleId == null) {
            throw new IllegalArgumentException("skillId and roleId must not be null for empty cell");
        }
        return new MatrixCell(skillId, roleId, null, true);
    }

    /**
     * Get CSS class for level badge.
     *
     * @return CSS class name (e.g., "level-basic") or empty string if cell is empty
     */
    public String getLevelCssClass() {
        if (isEmpty || requiredLevel == null) {
            return "";
        }
        return "level-" + requiredLevel.name().toLowerCase();
    }

    /**
     * Get display text for cell.
     *
     * @return Proficiency level display name or empty string if cell is empty
     */
    public String getDisplayText() {
        if (isEmpty || requiredLevel == null) {
            return "";
        }
        return requiredLevel.getDisplayName();
    }

    // Validation in canonical constructor
    public MatrixCell {
        if (skillId == null || roleId == null) {
            throw new IllegalArgumentException("skillId and roleId must not be null");
        }
        if (isEmpty && requiredLevel != null) {
            throw new IllegalArgumentException("Empty cells must not have a requiredLevel");
        }
        if (!isEmpty && requiredLevel == null) {
            throw new IllegalArgumentException("Non-empty cells must have a requiredLevel");
        }
    }
}
