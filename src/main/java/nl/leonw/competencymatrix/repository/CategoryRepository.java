package nl.leonw.competencymatrix.repository;

import nl.leonw.competencymatrix.model.CompetencyCategory;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends CrudRepository<CompetencyCategory, Integer> {

    @Query("SELECT * FROM competency_category ORDER BY display_order, name")
    List<CompetencyCategory> findAllOrderByDisplayOrder();

    Optional<CompetencyCategory> findByName(String name);
}
