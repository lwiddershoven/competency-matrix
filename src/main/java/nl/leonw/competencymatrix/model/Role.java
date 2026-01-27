package nl.leonw.competencymatrix.model;

/**
 * Career role with seniority level embedded in name.
 * UPDATED: Added roleFamily and seniorityOrder for matrix grouping (Feature 004).
 */
public record Role(
        Integer id,
        String name,
        String description,
        String roleFamily,       // NEW: "Developer", "Architect", "Operations"
        Integer seniorityOrder   // NEW: 1=Junior, 2=Medior, 3=Senior, etc.
) {
    public Role(String name, String description, String roleFamily, Integer seniorityOrder) {
        this(null, name, description, roleFamily, seniorityOrder);
    }

    // Legacy constructor for backward compatibility
    public Role(String name, String description) {
        this(null, name, description, "Other", 999);
    }
}
