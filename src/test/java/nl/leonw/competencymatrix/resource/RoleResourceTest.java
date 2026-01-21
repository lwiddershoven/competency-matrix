package nl.leonw.competencymatrix.resource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
class RoleResourceTest {

    @Inject
    RoleRepository roleRepository;

    @Test
    void shouldLoadRoleDetailPage() {
        Role role = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));

        given()
            .when().get("/roles/{id}", role.id())
            .then()
                .statusCode(200)
                .body(containsString("Junior Developer"));
    }

    @Test
    void shouldReturn404ForNonExistentRole() {
        given()
            .when().get("/roles/{id}", 99999)
            .then()
                .statusCode(404);
    }

    @Test
    void shouldLoadCategoriesFragment() {
        Role role = roleRepository.findByName("Junior Developer")
                .orElseGet(() -> roleRepository.save(new Role("Junior Developer", "Entry level")));

        given()
            .header("HX-Request", "true")
            .when().get("/roles/{id}/categories", role.id())
            .then()
                .statusCode(200);
    }
}
