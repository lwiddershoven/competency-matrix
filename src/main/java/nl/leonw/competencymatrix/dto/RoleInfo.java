package nl.leonw.competencymatrix.dto;

/**
 * Simple metadata for a role in the matrix.
 * Used for ordering and display purposes.
 */
public record RoleInfo(
    Integer id,
    String name,
    String family
) {
    public RoleInfo {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
        if (family == null || family.isBlank()) {
            throw new IllegalArgumentException("family must not be null or blank");
        }
    }
}
