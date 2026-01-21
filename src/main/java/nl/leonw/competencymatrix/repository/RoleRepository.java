package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.Role;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<Role, Integer> {

    @Query("SELECT * FROM rolename ORDER BY name")
    List<Role> findAllOrderByName();

    Optional<Role> findByName(String name);

    @Query("""
            SELECT r.* FROM rolename r
            JOIN role_progression rp ON r.id = rp.to_role_id
            WHERE rp.from_role_id = :roleId
            ORDER BY r.name
            """)
    List<Role> findNextRoles(Integer roleId);

    @Query("""
            SELECT r.* FROM rolename r
            JOIN role_progression rp ON r.id = rp.from_role_id
            WHERE rp.to_role_id = :roleId
            ORDER BY r.name
            """)
    List<Role> findPreviousRoles(Integer roleId);
}
