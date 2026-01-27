package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Skill;

import java.util.List;
import java.util.Optional;

/**
 * A single row in the matrix representing a skill and its proficiency requirements across roles.
 *
 * Feature: 004-matrix-overview
 * Purpose: Groups all cells for a skill in one row for tabular display
 */
public record MatrixRow(
    Skill skill,
    List<MatrixCell> cells
) {
    /**
     * Get the cell for a specific role.
     *
     * @param roleId The role ID to find
     * @return Optional containing the cell if found, empty otherwise
     */
    public Optional<MatrixCell> getCellForRole(Integer roleId) {
        if (roleId == null) {
            return Optional.empty();
        }
        return cells.stream()
            .filter(cell -> roleId.equals(cell.roleId()))
            .findFirst();
    }

    // Validation in canonical constructor
    public MatrixRow {
        if (skill == null) {
            throw new IllegalArgumentException("skill must not be null");
        }
        if (cells == null) {
            throw new IllegalArgumentException("cells must not be null");
        }
        // Defensive copy to ensure immutability
        cells = List.copyOf(cells);
    }
}
