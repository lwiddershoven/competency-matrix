package nl.leonw.competencymatrix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("role_progression")
public record RoleProgression(
        @Id Integer id,
        @Column("from_role_id") Integer fromRoleId,
        @Column("to_role_id") Integer toRoleId
) {
    public RoleProgression(Integer fromRoleId, Integer toRoleId) {
        this(null, fromRoleId, toRoleId);
    }
}
