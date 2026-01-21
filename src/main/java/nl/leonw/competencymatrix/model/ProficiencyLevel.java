package nl.leonw.competencymatrix.model;

public enum ProficiencyLevel {
    BASIC("Basic"),
    DECENT("Decent"),
    GOOD("Good"),
    EXCELLENT("Excellent");

    private final String displayName;

    ProficiencyLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOrdinalValue() {
        return ordinal() + 1;
    }

    public static ProficiencyLevel fromString(String value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.toUpperCase());
    }
}
