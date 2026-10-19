package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientSearchTest {

    @Inject
    PatientTestCleanup cleanup;

    private Long patientId;

    private void createTestPatient() {
        patientId = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "SearchUniqueAnna",
                    "lastName": "SearchUniqueKarlsson",
                    "birthYear": 1995
                }
                """)
            .when()
                .post("/api/patients")
            .then()
                .statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    @AfterEach
    void cleanUp() {
        if (patientId != null) {
            cleanup.deletePatient(patientId);
        }
    }

    @Test
    public void shouldFindPatientByFirstName() {
        createTestPatient();

        given()
            .queryParam("name", "SearchUniqueAnna")
        .when()
            .get("/api/patients/search")
        .then()
            .statusCode(200)
            .body("firstName", hasItem("SearchUniqueAnna"));
    }

    @Test
    public void shouldFindPatientByLastName() {
        createTestPatient();

        given()
            .queryParam("name", "SearchUniqueKarlsson")
        .when()
            .get("/api/patients/search")
        .then()
            .statusCode(200)
            .body("lastName", hasItem("SearchUniqueKarlsson"));
    }

    @Test
    public void shouldSearchCaseInsensitive() {
        createTestPatient();

        given()
            .queryParam("name", "searchuniqueanna")
        .when()
            .get("/api/patients/search")
        .then()
            .statusCode(200)
            .body("firstName", hasItem("SearchUniqueAnna"));
    }

    @Test
    public void shouldReturnEmptyListWhenNoMatch() {
        given()
            .queryParam("name", "XYZ_NO_MATCH_98765")
        .when()
            .get("/api/patients/search")
        .then()
            .statusCode(200)
            .body("size()", is(0));
    }

    @Test
    public void shouldRejectMissingSearchName() {
        given()
        .when()
            .get("/api/patients/search")
        .then()
            .statusCode(400);
    }
}