package nl.leonw.competencymatrix.validation;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * T064: Verify health endpoints - FR-009 compliance
 * Tests that health endpoints respond correctly at the management port (9000)
 */
@QuarkusTest
class HealthEndpointTest {

    @Test
    void shouldRespondToHealthEndpoint() {
        given()
            .port(9000)
            .when().get("/health")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
    }

    @Test
    void shouldRespondToLivenessEndpoint() {
        given()
            .port(9000)
            .when().get("/health/live")
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void shouldRespondToReadinessEndpoint() {
        given()
            .port(9000)
            .when().get("/health/ready")
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void shouldIncludeDatabaseHealthCheck() {
        given()
            .port(9000)
            .when().get("/health")
            .then()
                .statusCode(200)
                .body("checks.find { it.name == 'Database connections health check' }.status", is("UP"));
    }
}
