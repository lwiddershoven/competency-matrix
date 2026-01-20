package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.RoleSkillRequirement;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleSkillRequirementRepository extends CrudRepository<RoleSkillRequirement, Integer> {

    List<RoleSkillRequirement> findByRoleId(Integer roleId);

    Optional<RoleSkillRequirement> findByRoleIdAndSkillId(Integer roleId, Integer skillId);

    Optional<RoleSkillRequirement> findByRoleIdAndSkillIdAndRequiredLevel(Integer roleId, Integer skillId, String requiredLevel);
}
