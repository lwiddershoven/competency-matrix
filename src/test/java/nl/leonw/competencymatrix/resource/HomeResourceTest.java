package nl.leonw.competencymatrix.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class HomeResourceTest {

    @Test
    void shouldLoadHomePage() {
        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("Career Competency Matrix"));
    }

    @Test
    void shouldToggleTheme() {
        // First toggle: should set theme to dark
        given()
            .redirects().follow(false)
            .when().post("/theme")
            .then()
                .statusCode(303)
                .header("Location", containsString("/"))
                .cookie("theme", "dark");

        // Second toggle: should set theme to light
        given()
            .redirects().follow(false)
            .cookie("theme", "dark")
            .when().post("/theme")
            .then()
                .statusCode(303)
                .header("Location", containsString("/"))
                .cookie("theme", "light");
    }
}
