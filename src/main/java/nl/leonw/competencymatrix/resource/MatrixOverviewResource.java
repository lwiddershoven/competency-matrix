package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import nl.leonw.competencymatrix.config.CompetencySyncService;
import nl.leonw.competencymatrix.config.SyncResult;
import nl.leonw.competencymatrix.dto.MatrixViewModel;
import nl.leonw.competencymatrix.model.CompetencyCategory;
import nl.leonw.competencymatrix.repository.CategoryRepository;
import nl.leonw.competencymatrix.service.CompetencyService;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * REST resource for matrix overview page. Feature: 004-matrix-overview Task: T021 - Matrix overview endpoint
 */
@Path("/matrix")
public class MatrixOverviewResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatrixOverviewResource.class);

    Template matrixOverview;

    Template matrixTooltip;

    CompetencyService competencyService;

    CompetencySyncService competencySyncService;

    CategoryRepository categoryRepository;

    boolean allowReload;

    public MatrixOverviewResource(Template matrixOverview,
        @Location("fragments/matrix-tooltip") Template matrixTooltip,
        CompetencyService competencyService,
        CompetencySyncService competencySyncService,
        CategoryRepository categoryRepository,
        @ConfigProperty(name = "competence.sync.allow.reload", defaultValue = "false") boolean allowReload
    ) {
        this.matrixOverview = matrixOverview;
        this.matrixTooltip = matrixTooltip;
        this.competencyService = competencyService;
        this.competencySyncService = competencySyncService;
        this.categoryRepository = categoryRepository;
        this.allowReload = allowReload;
    }

    /**
     * Display complete competency matrix with all skills and roles. Supports optional category filtering via query
     * parameter.
     *
     * @param categoryId Optional category ID to filter skills (null = show all)
     * @param theme      User's theme preference from cookie
     * @return Matrix overview page
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getMatrixOverview(@QueryParam("category") Integer categoryId,
        @CookieParam("theme") @DefaultValue("light") String theme) {
        MatrixViewModel matrix = competencyService.buildMatrixViewModel(categoryId);

        LOGGER.info("Reload enabled: {}", allowReload);
        return matrixOverview
            .data("matrix", matrix)
            .data("theme", theme)
            .data("allowReload", allowReload);
    }

    /**
     * Get tooltip content for a skill showing all proficiency levels. Task: T032 - Tooltip endpoint for User Story 2
     *
     * @param skillId ID of the skill
     * @param level   Proficiency level (BASIS, REDELIJK, GOED, UITSTEKEND) - used to highlight the current level
     * @param theme   User's theme preference from cookie
     * @return Tooltip HTML fragment with all skill levels
     */
    @GET
    @Path("tooltips/skill/{skillId}")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance getSkillTooltip(@PathParam("skillId") Integer skillId,
        @QueryParam("level") String level,
        @CookieParam("theme") @DefaultValue("light") String theme) {
        nl.leonw.competencymatrix.model.Skill skill = competencyService.getSkillById(skillId)
            .orElseThrow(() -> new NotFoundException("Skill not found: " + skillId));

        // Parse the current level for highlighting
        nl.leonw.competencymatrix.model.ProficiencyLevel currentLevel;
        try {
            currentLevel = nl.leonw.competencymatrix.model.ProficiencyLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid proficiency level: " + level);
        }

        // Pass all levels to the template
        return matrixTooltip
            .data("skill", skill)
            .data("currentLevel", currentLevel)
            .data("levels", nl.leonw.competencymatrix.model.ProficiencyLevel.values())
            .data("theme", theme);
    }

    /**
     * Reload the database from seed files. Deletes all existing competency data and reloads from YAML files. Returns
     * JSON response with sync results and redirect URL to preserve category filter.
     *
     * @param selectedCategoryName Selected category name from form parameter (may be null)
     * @return JSON response with success status, sync details, and redirect URL
     */
    @POST
    @Path("reload")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadDatabase(@FormParam("selectedCategoryName") String selectedCategoryName) {
        try {
            // Reload from seed files using REPLACE mode
            SyncResult result = competencySyncService.syncFromConfiguration();

            // Build redirect URL with preserved category filter
            String redirectUrl = buildRedirectUrl(selectedCategoryName);

            // Return success response with redirect URL
            ReloadResponse response = new ReloadResponse(
                true,
                "Database reloaded successfully",
                redirectUrl,
                new ReloadDetails(
                    result.categoriesAdded() + result.categoriesUpdated(),
                    result.skillsAdded() + result.skillsUpdated(),
                    result.rolesAdded() + result.rolesUpdated(),
                    result.requirementsAdded() + result.requirementsUpdated(),
                    result.progressionsAdded() + result.progressionsUpdated()
                )
            );

            return Response.ok(response).build();
        } catch (Exception e) {
            // Log error and return failure response
            LOGGER.error("Database reload failed", e);

            ReloadResponse response = new ReloadResponse(
                false,
                "Database reload failed: " + e.getMessage(),
                "/matrix",
                null
            );

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(response)
                .build();
        }
    }

    /**
     * Build redirect URL with category filter preservation.
     * Resolves category name to new ID after database reload.
     *
     * @param selectedCategoryName Category name from frontend (may be null)
     * @return URL to redirect to (/matrix or /matrix?category=NEW_ID)
     */
    private String buildRedirectUrl(String selectedCategoryName) {
        // No category selected - redirect to unfiltered matrix
        if (selectedCategoryName == null || selectedCategoryName.trim().isEmpty()) {
            return "/matrix";
        }

        // Try to find category by name (case-insensitive, trimmed)
        Optional<CompetencyCategory> category = categoryRepository.findByNameIgnoreCase(selectedCategoryName.trim());

        if (category.isPresent()) {
            // Category found - build URL with new ID
            return "/matrix?category=" + category.get().id();
        } else {
            // Category not found (deleted or renamed in seed files)
            // Fallback to unfiltered matrix
            LOGGER.warn("Category '{}' not found after reload, redirecting to unfiltered matrix", selectedCategoryName);
            return "/matrix";
        }
    }

    /**
     * Response DTO for database reload operation.
     */
    public record ReloadResponse(
        boolean success,
        String message,
        String redirectUrl,
        ReloadDetails details
    ) {

    }

    /**
     * Details about the reload operation.
     */
    public record ReloadDetails(
        int categoriesProcessed,
        int skillsProcessed,
        int rolesProcessed,
        int requirementsProcessed,
        int progressionsProcessed
    ) {

    }
}
