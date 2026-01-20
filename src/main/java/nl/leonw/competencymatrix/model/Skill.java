package nl.leonw.competencymatrix.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("skill")
public record Skill(
        @Id Integer id,
        String name,
        @Column("category_id") Integer categoryId,
        @Column("basic_description") String basicDescription,
        @Column("decent_description") String decentDescription,
        @Column("good_description") String goodDescription,
        @Column("excellent_description") String excellentDescription
) {
    public Skill(String name, Integer categoryId, String basicDescription,
                 String decentDescription, String goodDescription, String excellentDescription) {
        this(null, name, categoryId, basicDescription, decentDescription, goodDescription, excellentDescription);
    }

    public String getDescriptionForLevel(ProficiencyLevel level) {
        return switch (level) {
            case BASIC -> basicDescription;
            case DECENT -> decentDescription;
            case GOOD -> goodDescription;
            case EXCELLENT -> excellentDescription;
        };
    }
}
