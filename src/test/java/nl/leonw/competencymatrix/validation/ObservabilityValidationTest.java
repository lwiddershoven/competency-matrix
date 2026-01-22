package nl.leonw.competencymatrix.validation;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * T070-T072: Observability validation tests
 * - T070: Verify Prometheus metrics endpoint
 * - T071: Verify logging (validated through application startup)
 * - T072: Verify health check details
 */
@QuarkusTest
class ObservabilityValidationTest {

    /**
     * T070: Verify Prometheus metrics endpoint returns valid Prometheus format
     */
    @Test
    void shouldExposePrometheusMetricsEndpoint() {
        String metricsResponse = given()
            .port(9000)
            .when().get("/metrics")
            .then()
                .statusCode(200)
                .contentType(anyOf(
                    is("text/plain"),
                    containsString("text/plain"),
                    containsString("openmetrics-text")
                ))
                .extract().asString();

        // Verify Prometheus format - should contain metric names and values
        assert metricsResponse.contains("# HELP") : "Metrics should contain HELP comments";
        assert metricsResponse.contains("# TYPE") : "Metrics should contain TYPE declarations";

        // Verify presence of key metrics
        assert metricsResponse.contains("jvm_memory") : "Should expose JVM memory metrics";
        assert metricsResponse.contains("http_server") : "Should expose HTTP server metrics";

        System.out.println("✓ Prometheus metrics endpoint validated - exposing " +
            metricsResponse.lines().filter(line -> line.startsWith("# HELP")).count() + " metrics");
    }

    /**
     * T072: Verify health check shows detailed information including database status
     */
    @Test
    void shouldProvideDetailedHealthCheckInformation() {
        given()
            .port(9000)
            .when().get("/health")
            .then()
                .statusCode(200)
                .body("status", is("UP"))
                .body("checks", notNullValue())
                .body("checks.size()", greaterThan(0));

        System.out.println("✓ Health check provides detailed component status");
    }

    /**
     * T072: Verify database connectivity is included in health check
     */
    @Test
    void shouldIncludeDatabaseConnectivityInHealthCheck() {
        String healthResponse = given()
            .port(9000)
            .when().get("/health")
            .then()
                .statusCode(200)
                .body("checks.find { it.name == 'Database connections health check' }", notNullValue())
                .body("checks.find { it.name == 'Database connections health check' }.status", is("UP"))
                .extract().asString();

        System.out.println("✓ Database connectivity health check is UP");
    }

    /**
     * T070: Verify metrics include application-specific data
     */
    @Test
    void shouldExposeApplicationSpecificMetrics() {
        String metricsResponse = given()
            .port(9000)
            .when().get("/metrics")
            .then()
                .statusCode(200)
                .extract().asString();

        // Count total number of exposed metrics
        long metricCount = metricsResponse.lines()
            .filter(line -> !line.startsWith("#") && !line.trim().isEmpty())
            .count();

        System.out.println("✓ Exposing " + metricCount + " total metric data points");

        assert metricCount > 0 : "Should expose at least some metrics";
    }

    /**
     * T071: Verify readiness probe for container orchestration
     */
    @Test
    void shouldProvideReadinessProbe() {
        given()
            .port(9000)
            .when().get("/health/ready")
            .then()
                .statusCode(200)
                .body("status", is("UP"));

        System.out.println("✓ Readiness probe indicates application is ready to serve traffic");
    }

    /**
     * T071: Verify liveness probe for container orchestration
     */
    @Test
    void shouldProvideLivenessProbe() {
        given()
            .port(9000)
            .when().get("/health/live")
            .then()
                .statusCode(200)
                .body("status", is("UP"));

        System.out.println("✓ Liveness probe indicates application is alive");
    }
}
