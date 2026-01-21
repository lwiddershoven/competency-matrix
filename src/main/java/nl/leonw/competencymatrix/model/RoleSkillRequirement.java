package nl.leonw.competencymatrix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("role_skill_requirement")
public record RoleSkillRequirement(
        @Id Integer id,
        @Column("role_id") Integer roleId,
        @Column("skill_id") Integer skillId,
        @Column("required_level") String requiredLevel
) {
    public RoleSkillRequirement(Integer roleId, Integer skillId, String requiredLevel) {
        this(null, roleId, skillId, requiredLevel);
    }

    public ProficiencyLevel getProficiencyLevel() {
        return ProficiencyLevel.fromString(requiredLevel);
    }
}
