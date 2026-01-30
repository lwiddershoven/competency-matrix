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
            case BASIS -> basicDescription;
            case REDELIJK -> decentDescription;
            case GOED -> goodDescription;
            case UITSTEKEND -> excellentDescription;
        };
    }
}
