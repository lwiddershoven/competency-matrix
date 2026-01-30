package nl.leonw.competencymatrix.dto;

/**
 * Simple metadata for a skill in the matrix.
 * Used for ordering and display purposes.
 */
public record SkillInfo(
    Integer id,
    String name
) {
    public SkillInfo {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be null or blank");
        }
    }
}
