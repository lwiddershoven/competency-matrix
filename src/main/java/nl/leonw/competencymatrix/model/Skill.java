package nl.leonw.competencymatrix.model;

// Plain record - no framework annotations
public record Skill(
        Integer id,
        String name,
        Integer categoryId,
        String basicDescription,
        String decentDescription,
        String goodDescription,
        String excellentDescription
) {
    public Skill(String name, Integer categoryId, String basicDescription,
                 String decentDescription, String goodDescription, String excellentDescription) {
        this(null, name, categoryId, basicDescription, decentDescription, goodDescription, excellentDescription);
    }

    public String getDescriptionForLevel(ProficiencyLevel level) {
        return switch (level) {
            case BASIC -> basicDescription;
            case DECENT -> decentDescription;
            case GOOD -> goodDescription;
            case EXCELLENT -> excellentDescription;
        };
    }
}
