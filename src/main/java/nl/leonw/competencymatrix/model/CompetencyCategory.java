package nl.leonw.competencymatrix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("competency_category")
public record CompetencyCategory(
        @Id Integer id,
        String name,
        @Column("display_order") int displayOrder
) {
    public CompetencyCategory(String name, int displayOrder) {
        this(null, name, displayOrder);
    }
}
