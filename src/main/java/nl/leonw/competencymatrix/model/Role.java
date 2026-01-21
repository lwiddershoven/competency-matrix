package nl.leonw.competencymatrix.model;

// Plain record - no framework annotations
public record Role(
        Integer id,
        String name,
        String description
) {
    public Role(String name, String description) {
        this(null, name, description);
    }
}
