package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientPaginationTest {

    @Inject
    PatientTestCleanup cleanup;

    private Long firstId;
    private Long secondId;

    private Long createPatient(String name) {
        return given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "%s",
                    "lastName": "PaginationTest",
                    "birthYear": 1995
                }
                """.formatted(name))
        .when()
            .post("/api/patients")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    @AfterEach
    void cleanUp() {
        if (firstId != null) {
            cleanup.deletePatient(firstId);
        }
        if (secondId != null) {
            cleanup.deletePatient(secondId);
        }
    }

    @Test
    public void shouldReturnPaginatedPatients() {
        firstId = createPatient("PageFirst");
        secondId = createPatient("PageSecond");

        given()
            .queryParam("page", 0)
            .queryParam("size", 1)
        .when()
            .get("/api/patients/page")
        .then()
            .statusCode(200)
            .body("page", is(0))
            .body("size", is(1))
            .body("patients.size()", is(1))
            .body("totalPatients", greaterThanOrEqualTo(2));
    }

    @Test
    public void shouldRejectInvalidPageSize() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 0)
        .when()
            .get("/api/patients/page")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectNegativePage() {
        given()
            .queryParam("page", -1)
            .queryParam("size", 10)
        .when()
            .get("/api/patients/page")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectTooLargePageSize() {
        given()
            .queryParam("page", 0)
            .queryParam("size", 101)
        .when()
            .get("/api/patients/page")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldReturnPatientsInAscendingIdOrder() {
        firstId = createPatient("SortedFirst");
        secondId = createPatient("SortedSecond");

        var response = given()
            .queryParam("page", 0)
            .queryParam("size", 100)
        .when()
            .get("/api/patients/page")
        .then()
            .statusCode(200)
            .extract().response();

        java.util.List<Integer> ids =
            response.jsonPath().getList("patients.id", Integer.class);

        java.util.List<Integer> sorted =
            new java.util.ArrayList<>(ids);

        sorted.sort(Integer::compareTo);

        assertEquals(sorted, ids);
        assertTrue(ids.contains(firstId.intValue()));
        assertTrue(ids.contains(secondId.intValue()));
    }
}