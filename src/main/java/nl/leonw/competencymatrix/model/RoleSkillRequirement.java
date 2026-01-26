package nl.leonw.competencymatrix.model;

// Plain record - no framework annotations
public record RoleSkillRequirement(
        Integer id,
        Integer roleId,
        Integer skillId,
        String requiredLevel
) {
    public RoleSkillRequirement(Integer roleId, Integer skillId, String requiredLevel) {
        this(null, roleId, skillId, requiredLevel);
    }

    public ProficiencyLevel getProficiencyLevel() {
        return ProficiencyLevel.fromString(requiredLevel);
    }
}
