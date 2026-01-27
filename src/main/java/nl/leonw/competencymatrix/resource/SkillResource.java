package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.service.CompetencyService;

@Path("/skills")
public class SkillResource {

    @Inject
    Template skill;

    @Inject
    CompetencyService competencyService;

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