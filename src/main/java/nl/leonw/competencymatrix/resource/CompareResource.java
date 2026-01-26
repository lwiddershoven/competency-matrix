package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.service.CompetencyService;

@Path("/compare")
public class CompareResource {

    @Inject
    Template compare;

    @Inject
    @io.quarkus.qute.Location("fragments/comparison-table")
    Template comparisonTable;

    @Inject
    CompetencyService competencyService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance compare(@QueryParam("from") Integer from,
                                    @QueryParam("to") Integer to,
                                    @CookieParam("theme") @DefaultValue("light") String theme) {
        Role fromRole = competencyService.getRoleById(from)
                .orElseThrow(() -> new NotFoundException("From role not found"));
        Role toRole = competencyService.getRoleById(to)
                .orElseThrow(() -> new NotFoundException("To role not found"));

        return compare
                .data("fromRole", fromRole)
                .data("toRole", toRole)
                .data("allRoles", competencyService.getAllRoles())
                .data("theme", theme);
    }

    @GET
    @Path("skills")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance compareSkills(@QueryParam("from") Integer from,
                                         @QueryParam("to") Integer to,
                                         @HeaderParam("HX-Request") String hxRequest,
                                         @CookieParam("theme") @DefaultValue("light") String theme) {
        Role fromRole = competencyService.getRoleById(from)
                .orElseThrow(() -> new NotFoundException("From role not found"));
        Role toRole = competencyService.getRoleById(to)
                .orElseThrow(() -> new NotFoundException("To role not found"));

        return comparisonTable
                .data("comparisons", competencyService.compareRoles(from, to))
                .data("fromRole", fromRole)
                .data("toRole", toRole)
                .data("theme", theme);
    }
}
