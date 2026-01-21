package nl.leonw.competencymatrix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("rolename")
public record Role(
        @Id Integer id,
        String name,
        String description
) {
    public Role(String name, String description) {
        this(null, name, description);
    }
}
