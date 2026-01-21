package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.Skill;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends CrudRepository<Skill, Integer> {

    @Query("SELECT * FROM skill WHERE category_id = :categoryId ORDER BY name")
    List<Skill> findByCategoryId(Integer categoryId);

    Optional<Skill> findByNameAndCategoryId(String name, Integer categoryId);

    @Query("""
            SELECT s.* FROM skill s
            JOIN role_skill_requirement rsr ON s.id = rsr.skill_id
            WHERE rsr.role_id = :roleId
            ORDER BY s.name
            """)
    List<Skill> findByRoleId(Integer roleId);
}
