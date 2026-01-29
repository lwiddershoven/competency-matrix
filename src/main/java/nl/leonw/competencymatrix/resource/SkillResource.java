package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.service.CompetencyService;

@Path("/skills")
public class SkillResource {

    @Inject
    Template skill;

    @Inject
    Template skills;

    @Inject
    CompetencyService competencyService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance skillsOverview(@QueryParam("category") @DefaultValue("") String categoryId,
                                           @CookieParam("theme") @DefaultValue("light") String theme) {
        Integer categoryFilter = null;
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            try {
                categoryFilter = Integer.parseInt(categoryId);
            } catch (NumberFormatException e) {
                // Invalid category ID, show all skills
            }
        }

        return skills
                .data("skillsByCategory", competencyService.getAllSkillsByCategory(categoryFilter))
                .data("categories", competencyService.getAllCategories())
                .data("selectedCategoryId", categoryFilter != null ? categoryFilter.toString() : "")
                .data("theme", theme);
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance skillDetail(@PathParam("id") Integer id,
                                   @CookieParam("theme") @DefaultValue("light") String theme) {
        Skill skillEntity = competencyService.getSkillById(id)
                .orElseThrow(() -> new NotFoundException("Skill not found"));

        return skill
                .data("skill", skillEntity)
                .data("theme", theme);
    }
}