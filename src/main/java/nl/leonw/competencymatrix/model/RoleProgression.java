package nl.leonw.competencymatrix.model;

// Plain record - no framework annotations
public record RoleProgression(
        Integer id,
        Integer fromRoleId,
        Integer toRoleId
) {
    public RoleProgression(Integer fromRoleId, Integer toRoleId) {
        this(null, fromRoleId, toRoleId);
    }
}
