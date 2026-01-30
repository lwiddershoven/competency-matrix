package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.ProficiencyLevel;

/**
 * Cell data for a skill-role combination in the matrix.
 * Stored in a map with string keys (skill name -> role name -> cell data).
 */
public record CellData(
    ProficiencyLevel level,
    boolean isEmpty,
    Long skillId,
    Integer roleId
) {
    /**
     * Create cell with a proficiency requirement.
     */
    public static CellData withLevel(Long skillId, Integer roleId, ProficiencyLevel level) {
        if (skillId == null || roleId == null || level == null) {
            throw new IllegalArgumentException("skillId, roleId, and level must not be null");
        }
        return new CellData(level, false, skillId, roleId);
    }

    /**
     * Create empty cell (no requirement for this skill-role combination).
     */
    public static CellData empty(Long skillId, Integer roleId) {
        if (skillId == null || roleId == null) {
            throw new IllegalArgumentException("skillId and roleId must not be null");
        }
        return new CellData(null, true, skillId, roleId);
    }

    /**
     * Get CSS class for level badge.
     */
    public String getLevelCssClass() {
        if (isEmpty || level == null) {
            return "";
        }
        return "level-" + level.name().toLowerCase();
    }

    /**
     * Get display text for the cell.
     */
    public String getDisplayText() {
        if (isEmpty || level == null) {
            return "";
        }
        return level.getDisplayName();
    }

    public CellData {
        if (skillId == null || roleId == null) {
            throw new IllegalArgumentException("skillId and roleId must not be null");
        }
        if (isEmpty && level != null) {
            throw new IllegalArgumentException("Empty cells must not have a level");
        }
        if (!isEmpty && level == null) {
            throw new IllegalArgumentException("Non-empty cells must have a level");
        }
    }
}
