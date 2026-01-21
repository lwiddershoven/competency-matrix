package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.RoleProgression;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleProgressionRepository extends CrudRepository<RoleProgression, Integer> {

    Optional<RoleProgression> findByFromRoleIdAndToRoleId(Integer fromRoleId, Integer toRoleId);
}
