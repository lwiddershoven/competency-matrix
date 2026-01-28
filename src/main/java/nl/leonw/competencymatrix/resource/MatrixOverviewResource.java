package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import nl.leonw.competencymatrix.dto.MatrixViewModel;
import nl.leonw.competencymatrix.service.CompetencyService;

/**
 * REST resource for matrix overview page.
 * Feature: 004-matrix-overview
 * Task: T021 - Matrix overview endpoint
 */
@Path("/matrix")
public class MatrixOverviewResource {

    @Inject
    Template matrixOverview;

    @Inject
    @io.quarkus.qute.Location("fragments/matrix-tooltip")
    Template matrixTooltip;

    @Inject
    CompetencyService competencyService;

    /**
     * Display complete competency matrix with all skills and roles.
     * Supports optional category filtering via query parameter.
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @param theme User's theme preference from cookie
     * @return Matrix overview page
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMatrixOverview(@QueryParam("category") Integer categoryId,
                                             @CookieParam("theme") @DefaultValue("light") String theme) {
        MatrixViewModel matrix = competencyService.buildMatrixViewModel(categoryId);
        return matrixOverview
                .data("matrix", matrix)
                .data("theme", theme);
    }

    /**
     * Get tooltip content for a skill at a specific proficiency level.
     * Task: T032 - Tooltip endpoint for User Story 2
     *
     * @param skillId ID of the skill
     * @param level Proficiency level (BASIC, DECENT, GOOD, EXCELLENT)
     * @param theme User's theme preference from cookie
     * @return Tooltip HTML fragment with skill details
     */
    @GET
    @Path("tooltips/skill/{skillId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getSkillTooltip(@PathParam("skillId") Integer skillId,
                                           @QueryParam("level") String level,
                                           @CookieParam("theme") @DefaultValue("light") String theme) {
        nl.leonw.competencymatrix.model.Skill skill = competencyService.getSkillById(skillId)
                .orElseThrow(() -> new NotFoundException("Skill not found: " + skillId));

        // Get description for the specified level
        nl.leonw.competencymatrix.model.ProficiencyLevel proficiencyLevel;
        try {
            proficiencyLevel = nl.leonw.competencymatrix.model.ProficiencyLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid proficiency level: " + level);
        }

        String description = skill.getDescriptionForLevel(proficiencyLevel);

        return matrixTooltip
                .data("skill", skill)
                .data("level", proficiencyLevel)
                .data("description", description)
                .data("theme", theme);
    }
}
