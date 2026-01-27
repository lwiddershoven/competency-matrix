package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Role;

/**
 * Column header for a role in the matrix.
 * <p>
 * Feature: 004-matrix-overview Purpose: Provides role display metadata for matrix column headers
 */
public record MatrixColumnHeader(
    Role role
) {

    /**
     * Create column header from role with automatic abbreviation generation.
     *
     * @param role The role to create header for
     * @return MatrixColumnHeader with role and generated abbreviation
     */
    public static MatrixColumnHeader from(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        return new MatrixColumnHeader(role);
    }

    /**
     * Whether to show tooltip with full name (true if name >15 chars).
     *
     * @return true if role name is long and needs tooltip
     */
    public boolean needsTooltip() {
        return role != null && role.name() != null && role.name().length() > 15;
    }

    // Validation in canonical constructor
    public MatrixColumnHeader {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
    }
}
