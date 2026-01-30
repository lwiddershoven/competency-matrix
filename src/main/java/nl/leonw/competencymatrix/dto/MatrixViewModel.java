package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.CompetencyCategory;

import java.util.List;
import java.util.Map;

/**
 * View model for the competency matrix overview page.
 * Uses CompetencyMatrix with explicit get(roleName, skillName) lookups.
 * Provides ordered lists of skills and roles with IDs for rendering.
 */
public record MatrixViewModel(
    CompetencyMatrix matrix,
    List<SkillInfo> skillsInOrder,
    List<RoleInfo> rolesInOrder,
    Map<String, List<RoleInfo>> rolesByFamily,
    List<CompetencyCategory> categories,
    String selectedCategoryId
) {
    /**
     * Create unfiltered matrix view model (all skills).
     */
    public static MatrixViewModel unfiltered(
        CompetencyMatrix matrix,
        List<SkillInfo> skillsInOrder,
        List<RoleInfo> rolesInOrder,
        Map<String, List<RoleInfo>> rolesByFamily,
        List<CompetencyCategory> categories
    ) {
        if (matrix == null || skillsInOrder == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        return new MatrixViewModel(matrix, skillsInOrder, rolesInOrder, rolesByFamily, categories, null);
    }

    /**
     * Create filtered matrix view model (skills from specific category).
     */
    public static MatrixViewModel filtered(
        CompetencyMatrix matrix,
        List<SkillInfo> skillsInOrder,
        List<RoleInfo> rolesInOrder,
        Map<String, List<RoleInfo>> rolesByFamily,
        List<CompetencyCategory> categories,
        String selectedCategoryId
    ) {
        if (matrix == null || skillsInOrder == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            throw new IllegalArgumentException("selectedCategoryId must not be null or empty for filtered view");
        }
        return new MatrixViewModel(matrix, skillsInOrder, rolesInOrder, rolesByFamily, categories, selectedCategoryId);
    }

    /**
     * Check if a category filter is currently active.
     */
    public boolean hasFilter() {
        return selectedCategoryId != null && !selectedCategoryId.isEmpty();
    }

    // Validation in canonical constructor
    public MatrixViewModel {
        if (matrix == null || skillsInOrder == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        // Defensive copies to ensure immutability
        skillsInOrder = List.copyOf(skillsInOrder);
        rolesInOrder = List.copyOf(rolesInOrder);
        rolesByFamily = Map.copyOf(rolesByFamily);
        categories = List.copyOf(categories);
    }
}
