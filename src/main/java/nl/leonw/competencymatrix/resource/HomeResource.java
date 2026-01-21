package nl.leonw.competencymatrix.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import nl.leonw.competencymatrix.service.CompetencyService;

@Path("/")
public class HomeResource {

    @Inject
    Template index;

    @Inject
    CompetencyService competencyService;

    @GET
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance home(@CookieParam("theme") @DefaultValue("light") String theme) {
        return index
                .data("roles", competencyService.getAllRoles())
                .data("theme", theme);
    }

    @POST
    @Path("theme")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public Response toggleTheme(@CookieParam("theme") @DefaultValue("light") String currentTheme) {
        String newTheme = "dark".equals(currentTheme) ? "light" : "dark";
        NewCookie cookie = new NewCookie.Builder("theme")
                .value(newTheme)
                .path("/")
                .maxAge(60 * 60 * 24 * 365)
                .build();
        return Response.seeOther(java.net.URI.create("/")).cookie(cookie).build();
    }
}
