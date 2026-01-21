package nl.leonw.competencymatrix.model;

// Plain record - no framework annotations
public record CompetencyCategory(
        Integer id,
        String name,
        int displayOrder
) {
    public CompetencyCategory(String name, int displayOrder) {
        this(null, name, displayOrder);
    }
}
