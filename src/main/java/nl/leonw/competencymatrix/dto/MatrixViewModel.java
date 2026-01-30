package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.CompetencyCategory;

import java.util.List;
import java.util.Map;

/**
 * View model for the competency matrix overview page.
 * Uses a map of maps structure with string keys for readability and alignment.
 * Structure: skillName -> (roleName -> cellData)
 */
public record MatrixViewModel(
    Map<String, Map<String, CellData>> matrix,
    List<RoleInfo> rolesInOrder,
    Map<String, List<RoleInfo>> rolesByFamily,
    List<CompetencyCategory> categories,
    String selectedCategoryId
) {
    /**
     * Create unfiltered matrix view model (all skills).
     */
    public static MatrixViewModel unfiltered(
        Map<String, Map<String, CellData>> matrix,
        List<RoleInfo> rolesInOrder,
        Map<String, List<RoleInfo>> rolesByFamily,
        List<CompetencyCategory> categories
    ) {
        if (matrix == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        return new MatrixViewModel(matrix, rolesInOrder, rolesByFamily, categories, null);
    }

    /**
     * Create filtered matrix view model (skills from specific category).
     */
    public static MatrixViewModel filtered(
        Map<String, Map<String, CellData>> matrix,
        List<RoleInfo> rolesInOrder,
        Map<String, List<RoleInfo>> rolesByFamily,
        List<CompetencyCategory> categories,
        String selectedCategoryId
    ) {
        if (matrix == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            throw new IllegalArgumentException("selectedCategoryId must not be null or empty for filtered view");
        }
        return new MatrixViewModel(matrix, rolesInOrder, rolesByFamily, categories, selectedCategoryId);
    }

    /**
     * Check if a category filter is currently active.
     */
    public boolean hasFilter() {
        return selectedCategoryId != null && !selectedCategoryId.isEmpty();
    }

    /**
     * Get all skill names in sorted order.
     */
    public List<String> getSkillNames() {
        return matrix.keySet().stream()
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    }

    /**
     * Get cell data for a specific skill and role.
     */
    public CellData getCell(String skillName, String roleName) {
        Map<String, CellData> skillRow = matrix.get(skillName);
        if (skillRow == null) {
            return null;
        }
        return skillRow.get(roleName);
    }

    // Validation in canonical constructor
    public MatrixViewModel {
        if (matrix == null || rolesInOrder == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("All parameters must not be null");
        }
        // Defensive copies to ensure immutability
        matrix = Map.copyOf(matrix);
        rolesInOrder = List.copyOf(rolesInOrder);
        rolesByFamily = Map.copyOf(rolesByFamily);
        categories = List.copyOf(categories);
    }
}
