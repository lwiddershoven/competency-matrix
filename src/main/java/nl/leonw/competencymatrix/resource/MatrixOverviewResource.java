package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
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
}
