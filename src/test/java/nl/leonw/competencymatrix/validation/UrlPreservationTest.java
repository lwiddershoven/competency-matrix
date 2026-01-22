package nl.leonw.competencymatrix.validation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * T063: Test bookmark URLs - Verify all URLs from baseline resolve correctly per FR-002 and SC-008
 */
@QuarkusTest
class UrlPreservationTest {

    @Inject
    RoleRepository roleRepository;

    @Test
    void shouldPreserveHomeUrl() {
        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("Career Competency Matrix"));
    }

    @Test
    void shouldPreserveRoleDetailUrls() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();

        given()
            .when().get("/roles/{id}", juniorDev.id())
            .then()
                .statusCode(200)
                .body(containsString("Junior Developer"));
    }

    @Test
    void shouldPreserveCompareUrls() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();
        Role seniorDev = roleRepository.findByName("Senior Developer").orElseThrow();

        given()
            .queryParam("from", juniorDev.id())
            .queryParam("to", seniorDev.id())
            .when().get("/compare")
            .then()
                .statusCode(200)
                .body(containsString("Compare Roles"));
    }

    @Test
    void shouldPreserveRoleCategoriesFragmentUrl() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();

        given()
            .header("HX-Request", "true")
            .when().get("/roles/{id}/categories", juniorDev.id())
            .then()
                .statusCode(200);
    }

    @Test
    void shouldPreserveCompareSkillsFragmentUrl() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();
        Role seniorDev = roleRepository.findByName("Senior Developer").orElseThrow();

        given()
            .queryParam("from", juniorDev.id())
            .queryParam("to", seniorDev.id())
            .header("HX-Request", "true")
            .when().get("/compare/skills")
            .then()
                .statusCode(200);
    }
}
