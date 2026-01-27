package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.CompetencyCategory;

import java.util.List;
import java.util.Map;

/**
 * View model for the competency matrix overview page.
 *
 * Feature: 004-matrix-overview
 * Purpose: Provides complete matrix data with role grouping and category filtering support
 */
public record MatrixViewModel(
    List<MatrixRow> rows,
    Map<String, List<MatrixColumnHeader>> rolesByFamily,
    List<CompetencyCategory> categories,
    String selectedCategoryId
) {
    /**
     * Create unfiltered matrix view model (all skills).
     *
     * @param rows All skill rows in the matrix
     * @param rolesByFamily Roles grouped by family (Developer, Architect, Operations)
     * @param categories All available categories for filtering
     * @return MatrixViewModel with no category filter applied
     */
    public static MatrixViewModel unfiltered(
        List<MatrixRow> rows,
        Map<String, List<MatrixColumnHeader>> rolesByFamily,
        List<CompetencyCategory> categories
    ) {
        if (rows == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("rows, rolesByFamily, and categories must not be null");
        }
        return new MatrixViewModel(rows, rolesByFamily, categories, null);
    }

    /**
     * Create filtered matrix view model (skills from specific category).
     *
     * @param rows Filtered skill rows in the matrix
     * @param rolesByFamily Roles grouped by family (Developer, Architect, Operations)
     * @param categories All available categories for filtering
     * @param selectedCategoryId ID of the currently selected category
     * @return MatrixViewModel with category filter applied
     */
    public static MatrixViewModel filtered(
        List<MatrixRow> rows,
        Map<String, List<MatrixColumnHeader>> rolesByFamily,
        List<CompetencyCategory> categories,
        String selectedCategoryId
    ) {
        if (rows == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("rows, rolesByFamily, and categories must not be null");
        }
        if (selectedCategoryId == null || selectedCategoryId.isEmpty()) {
            throw new IllegalArgumentException("selectedCategoryId must not be null or empty for filtered view");
        }
        return new MatrixViewModel(rows, rolesByFamily, categories, selectedCategoryId);
    }

    /**
     * Check if a category filter is currently active.
     *
     * @return true if a category filter is applied, false otherwise
     */
    public boolean hasFilter() {
        return selectedCategoryId != null && !selectedCategoryId.isEmpty();
    }

    // Validation in canonical constructor
    public MatrixViewModel {
        if (rows == null || rolesByFamily == null || categories == null) {
            throw new IllegalArgumentException("rows, rolesByFamily, and categories must not be null");
        }
        // Defensive copies to ensure immutability
        rows = List.copyOf(rows);
        rolesByFamily = Map.copyOf(rolesByFamily);
        categories = List.copyOf(categories);
    }
}
