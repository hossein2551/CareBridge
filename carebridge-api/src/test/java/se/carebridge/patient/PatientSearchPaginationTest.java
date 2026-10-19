
package se.carebridge.patient;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientSearchPaginationTest {

    @Inject
    PatientTestCleanup cleanup;

    private final List<Long> createdPatientIds = new ArrayList<>();

    private Long createPatient(String firstName) {
        Long id = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "%s",
                    "lastName": "SearchPaginationTest",
                    "birthYear": 1995
                }
                """.formatted(firstName))
        .when()
            .post("/api/patients")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        createdPatientIds.add(id);
        return id;
    }

    @AfterEach
    void cleanUp() {
        for (Long id : createdPatientIds) {
            cleanup.deletePatient(id);
        }
        createdPatientIds.clear();
    }

    @Test
    public void shouldFindMatchingPatient() {
        String name = "SearchPage" +
            UUID.randomUUID().toString().replace("-", "");

        createPatient(name);

        given()
            .queryParam("name", name)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(200)
            .body("patients.firstName", hasItem(name))
            .body("totalPatients", is(1))
            .body("page", is(0))
            .body("totalPages", is(1));
    }

    @Test
    public void shouldReturnZeroMatches() {
        String missingName = "Missing" +
            UUID.randomUUID().toString().replace("-", "");

        given()
            .queryParam("name", missingName)
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(200)
            .body("patients.size()", is(0))
            .body("totalPatients", is(0))
            .body("totalPages", is(0));
    }

    @Test
    public void shouldPaginateSearchResults() {
        String prefix = "PageFind" +
            UUID.randomUUID().toString().replace("-", "");

        String firstName = prefix + "First";
        String secondName = prefix + "Second";

        createPatient(firstName);
        createPatient(secondName);

        var firstPage = given()
            .queryParam("name", prefix)
            .queryParam("page", 0)
            .queryParam("size", 1)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(200)
            .body("page", is(0))
            .body("size", is(1))
            .body("totalPatients", is(2))
            .body("totalPages", is(2))
            .body("patients.size()", is(1))
            .extract()
            .response();

        var secondPage = given()
            .queryParam("name", prefix)
            .queryParam("page", 1)
            .queryParam("size", 1)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(200)
            .body("page", is(1))
            .body("totalPatients", is(2))
            .body("patients.size()", is(1))
            .extract()
            .response();

        Long firstId = firstPage.jsonPath()
            .getLong("patients[0].id");

        Long secondId = secondPage.jsonPath()
            .getLong("patients[0].id");

        assertNotEquals(firstId, secondId);
        assertTrue(firstId < secondId);
    }

    @Test
    public void shouldRejectNegativePage() {
        given()
            .queryParam("name", "Sara")
            .queryParam("page", -1)
            .queryParam("size", 10)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectInvalidPageSize() {
        given()
            .queryParam("name", "Sara")
            .queryParam("page", 0)
            .queryParam("size", 0)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectTooLargePageSize() {
        given()
            .queryParam("name", "Sara")
            .queryParam("page", 0)
            .queryParam("size", 101)
        .when()
            .get("/api/patients/search/page")
        .then()
            .statusCode(400);
    }
}
