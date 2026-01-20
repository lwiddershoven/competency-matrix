package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleSkillRequirementRepository extends CrudRepository<RoleSkillRequirement, Integer> {

    @Query("SELECT * FROM role_skill_requirement WHERE role_id = :roleId")
    List<RoleSkillRequirement> findByRoleId(Integer roleId);

    @Query("SELECT * FROM role_skill_requirement WHERE role_id = :roleId AND skill_id = :skillId")
    Optional<RoleSkillRequirement> findByRoleIdAndSkillId(Integer roleId, Integer skillId);

    Optional<RoleSkillRequirement> findByRoleIdAndSkillIdAndRequiredLevel(Integer roleId, Integer skillId, String requiredLevel);
}
