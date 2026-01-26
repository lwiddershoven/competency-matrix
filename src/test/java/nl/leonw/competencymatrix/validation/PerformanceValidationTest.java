package nl.leonw.competencymatrix.validation;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import nl.leonw.competencymatrix.model.Role;
import nl.leonw.competencymatrix.repository.RoleRepository;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * T066: Measure page load times - verify <2s per SC-003
 * Performance validation tests to ensure page loads meet performance requirements
 */
@QuarkusTest
class PerformanceValidationTest {

    @Inject
    RoleRepository roleRepository;

    @Test
    void shouldLoadHomePageWithinPerformanceThreshold() {
        long startTime = System.currentTimeMillis();

        given()
            .when().get("/")
            .then()
                .statusCode(200)
                .body(containsString("Career Competency Matrix"));

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Home page load time: " + elapsedTime + "ms");

        // Performance requirement: <2s (2000ms)
        // Allowing some buffer for test infrastructure overhead
        assert elapsedTime < 2000 : "Home page load time exceeded 2s threshold: " + elapsedTime + "ms";
    }

    @Test
    void shouldLoadRoleDetailPageWithinPerformanceThreshold() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();

        long startTime = System.currentTimeMillis();

        given()
            .when().get("/roles/{id}", juniorDev.id())
            .then()
                .statusCode(200)
                .body(containsString("Junior Developer"));

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Role detail page load time: " + elapsedTime + "ms");

        assert elapsedTime < 2000 : "Role detail page load time exceeded 2s threshold: " + elapsedTime + "ms";
    }

    @Test
    void shouldLoadComparePageWithinPerformanceThreshold() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();
        Role seniorDev = roleRepository.findByName("Senior Developer").orElseThrow();

        long startTime = System.currentTimeMillis();

        given()
            .queryParam("from", juniorDev.id())
            .queryParam("to", seniorDev.id())
            .when().get("/compare")
            .then()
                .statusCode(200)
                .body(containsString("Compare Roles"));

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Compare page load time: " + elapsedTime + "ms");

        assert elapsedTime < 2000 : "Compare page load time exceeded 2s threshold: " + elapsedTime + "ms";
    }

    @Test
    void shouldLoadRoleCategoriesFragmentWithinPerformanceThreshold() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();

        long startTime = System.currentTimeMillis();

        given()
            .header("HX-Request", "true")
            .when().get("/roles/{id}/categories", juniorDev.id())
            .then()
                .statusCode(200);

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Role categories fragment load time: " + elapsedTime + "ms");

        // Fragment should load faster than full pages
        assert elapsedTime < 1000 : "Fragment load time exceeded 1s threshold: " + elapsedTime + "ms";
    }

    @Test
    void shouldLoadCompareSkillsFragmentWithinPerformanceThreshold() {
        Role juniorDev = roleRepository.findByName("Junior Developer").orElseThrow();
        Role seniorDev = roleRepository.findByName("Senior Developer").orElseThrow();

        long startTime = System.currentTimeMillis();

        given()
            .queryParam("from", juniorDev.id())
            .queryParam("to", seniorDev.id())
            .header("HX-Request", "true")
            .when().get("/compare/skills")
            .then()
                .statusCode(200);

        long elapsedTime = System.currentTimeMillis() - startTime;
        System.out.println("Compare skills fragment load time: " + elapsedTime + "ms");

        assert elapsedTime < 1000 : "Fragment load time exceeded 1s threshold: " + elapsedTime + "ms";
    }
}
