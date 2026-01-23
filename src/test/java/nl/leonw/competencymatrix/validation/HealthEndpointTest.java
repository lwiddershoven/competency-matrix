package nl.leonw.competencymatrix.validation;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.http.TestHTTPResource;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * T064: Verify health endpoints - FR-009 compliance
 * Tests that health endpoints respond correctly at the management port (9000)
 */
@QuarkusTest
class HealthEndpointTest {

    @TestHTTPResource(value = "/health", management = true)
    String healthUrl;

    @TestHTTPResource(value = "/health/live", management = true)
    String livenessUrl;

    @TestHTTPResource(value = "/health/ready", management = true)
    String readinessUrl;

    @Test
    void shouldRespondToHealthEndpoint() {
        given()
            .when().get(healthUrl)
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue());
    }

    @Test
    void shouldRespondToLivenessEndpoint() {
        given()
            .when().get(livenessUrl)
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void shouldRespondToReadinessEndpoint() {
        given()
            .when().get(readinessUrl)
            .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void shouldIncludeDatabaseHealthCheck() {
        given()
            .when().get(healthUrl)
            .then()
                .statusCode(200)
                .body("checks.find { it.name == 'Database connections health check' }.status", is("UP"));
    }
}
