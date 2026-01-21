package nl.leonw.competencymatrix.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class CompareResourceTest {

    @Inject
    RoleRepository roleRepository;

    private Role fromRole;
    private Role toRole;

    @BeforeEach
    void setUp() {
        fromRole = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));
        toRole = roleRepository.findByName("Senior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Senior Developer", "Senior level")));
    }

    @Test
    void shouldLoadComparePage() {
        given()
            .queryParam("from", fromRole.id())
            .queryParam("to", toRole.id())
            .when().get("/compare")
            .then()
                .statusCode(200)
                .body(containsString("Compare Roles"));
    }

    @Test
    void shouldReturn404ForNonExistentFromRole() {
        given()
            .queryParam("from", 99999)
            .queryParam("to", toRole.id())
            .when().get("/compare")
            .then()
                .statusCode(404);
    }

    @Test
    void shouldLoadComparisonFragment() {
        given()
            .queryParam("from", fromRole.id())
            .queryParam("to", toRole.id())
            .header("HX-Request", "true")
            .when().get("/compare/skills")
            .then()
                .statusCode(200);
    }
}
