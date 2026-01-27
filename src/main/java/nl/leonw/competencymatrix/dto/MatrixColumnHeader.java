package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Role;

/**
 * Column header for a role in the matrix with abbreviation support.
 *
 * Feature: 004-matrix-overview
 * Purpose: Provides role display metadata for matrix column headers
 */
public record MatrixColumnHeader(
    Role role,
    String abbreviation
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
        String abbrev = generateAbbreviation(role.name());
        return new MatrixColumnHeader(role, abbrev);
    }

    /**
     * Generate abbreviation for role name.
     * Names ≤15 chars: return as-is
     * Names >15 chars: extract up to 3 uppercase letters
     *
     * Examples:
     * - "Junior Developer" → "Junior Developer" (≤15 chars)
     * - "Software Architect" → "SA" (2 uppercase letters)
     * - "Lead Developer / Software Architect" → "LDS" (3 uppercase letters)
     *
     * @param name Role name
     * @return Abbreviated name
     */
    private static String generateAbbreviation(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        if (name.length() <= 15) {
            return name;
        }

        // Extract uppercase letters (up to 3)
        StringBuilder abbrev = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c) && abbrev.length() < 3) {
                abbrev.append(c);
            }
        }

        // Fallback if no uppercase letters found: take first 3 chars
        if (abbrev.isEmpty()) {
            return name.substring(0, Math.min(3, name.length()));
        }

        return abbrev.toString();
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
        if (abbreviation == null || abbreviation.isEmpty()) {
            throw new IllegalArgumentException("abbreviation must not be null or empty");
        }
    }
}
