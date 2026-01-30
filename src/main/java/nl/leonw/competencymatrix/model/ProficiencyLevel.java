package nl.leonw.competencymatrix.model;

public enum ProficiencyLevel {
    BASIS("Basis"),
    REDELIJK("Redelijk"),
    GOED("Goed"),
    UITSTEKEND("Uitstekend");

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
