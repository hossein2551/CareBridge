package se.carebridge.healthcare;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.Response;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HealthcareUnitResourceTest {

    @Inject
    HealthcareUnitTestCleanup cleanup;

    private Long createdId;

    @AfterEach
    void cleanUp() {
        if (createdId != null) {
            cleanup.deleteUnit(createdId);
        }
    }

    @Test
    public void shouldCreateAndGetHealthcareUnit() {
        String code = "TEST-" + UUID.randomUUID();

        Response response = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "CareBridge Test Unit",
                    "city": "Kalmar",
                    "organizationCode": "%s"
                }
                """.formatted(code))
        .when()
            .post("/api/healthcare-units");

        response.then()
            .statusCode(201)
            .body("name", is("CareBridge Test Unit"))
            .body("createdAt", notNullValue());

        createdId = response.jsonPath().getLong("id");

        given()
        .when()
            .get("/api/healthcare-units/" + createdId)
        .then()
            .statusCode(200)
            .body("city", is("Kalmar"));
    }

    @Test
    public void shouldFindHealthcareUnitByCity() {
        String code = "TEST-" + UUID.randomUUID();

        Response response = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "Test Clinic",
                    "city": "Vaxjo",
                    "organizationCode": "%s"
                }
                """.formatted(code))
        .when()
            .post("/api/healthcare-units");

        response.then().statusCode(201);
        createdId = response.jsonPath().getLong("id");

        given()
            .queryParam("city", "vaxjo")
        .when()
            .get("/api/healthcare-units/search")
        .then()
            .statusCode(200)
            .body("name", hasItem("Test Clinic"));
    }

    @Test
    public void shouldRejectInvalidHealthcareUnit() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "name": "",
                    "city": "",
                    "organizationCode": ""
                }
                """)
        .when()
            .post("/api/healthcare-units")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldReturn404ForUnknownUnit() {
        given()
        .when()
            .get("/api/healthcare-units/999999999")
        .then()
            .statusCode(404);
    }
}