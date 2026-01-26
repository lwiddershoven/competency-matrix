package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import nl.leonw.competencymatrix.model.ProficiencyLevel;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.model.Skill;
import nl.leonw.competencymatrix.service.CompetencyService;

@Path("/roles")
public class RoleResource {

    @Inject
    Template role;

    @Inject
    @io.quarkus.qute.Location("fragments/category-section")
    Template categorySection;

    @Inject
    @io.quarkus.qute.Location("fragments/skill-modal")
    Template skillModal;

    @Inject
    CompetencyService competencyService;

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance roleDetail(@PathParam("id") Integer id,
                                      @CookieParam("theme") @DefaultValue("light") String theme) {
        Role roleEntity = competencyService.getRoleById(id)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        return role
                .data("role", roleEntity)
                .data("nextRoles", competencyService.getNextRoles(id))
                .data("previousRoles", competencyService.getPreviousRoles(id))
                .data("theme", theme);
    }

    @GET
    @Path("{id}/categories")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance roleCategories(@PathParam("id") Integer id,
                                          @HeaderParam("HX-Request") String hxRequest,
                                          @CookieParam("theme") @DefaultValue("light") String theme) {
        Role roleEntity = competencyService.getRoleById(id)
                .orElseThrow(() -> new NotFoundException("Role not found"));

        return categorySection
                .data("skillsByCategory", competencyService.getSkillsByCategoryForRole(id))
                .data("roleId", id)
                .data("theme", theme);
    }

    @GET
    @Path("{roleId}/skills/{skillId}")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance skillDetail(@PathParam("roleId") Integer roleId,
                                       @PathParam("skillId") Integer skillId,
                                       @HeaderParam("HX-Request") String hxRequest,
                                       @CookieParam("theme") @DefaultValue("light") String theme) {
        Skill skill = competencyService.getSkillById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found"));

        ProficiencyLevel requiredLevel = competencyService.getRequirementForRoleAndSkill(roleId, skillId)
                .map(req -> req.getProficiencyLevel())
                .orElse(null);

        return skillModal
                .data("skill", skill)
                .data("requiredLevel", requiredLevel)
                .data("theme", theme);
    }
}
