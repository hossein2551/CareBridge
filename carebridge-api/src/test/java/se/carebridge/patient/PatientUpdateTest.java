package se.carebridge.patient;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientUpdateTest {

    @Inject
    PatientTestCleanup cleanup;

    private Long createdPatientId;

    @AfterEach
    void cleanUp() {
        if (createdPatientId != null) {
            cleanup.deletePatient(createdPatientId);
        }
    }

    @Test
    public void shouldUpdatePatient() {
        createdPatientId = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Anna",
                    "lastName": "Test",
                    "birthYear": 1995
                }
                """)
            .when()
            .post("/api/patients")
            .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Anna",
                    "lastName": "Updated",
                    "birthYear": 1996
                }
                """)
            .when()
            .put("/api/patients/" + createdPatientId)
            .then()
            .statusCode(200)
            .body("lastName", is("Updated"));

        given()
            .when()
            .get("/api/patients/" + createdPatientId)
            .then()
            .statusCode(200)
            .body("lastName", is("Updated"))
            .body("birthYear", is(1996));
    }

    @Test
    public void shouldReturn404WhenUpdatingUnknownPatient() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Unknown",
                    "lastName": "Patient",
                    "birthYear": 1990
                }
                """)
            .when()
            .put("/api/patients/999999999")
            .then()
            .statusCode(404);
    }

    @Test
    public void shouldRejectInvalidPatientUpdate() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "",
                    "lastName": "Test",
                    "birthYear": 1800
                }
                """)
            .when()
            .put("/api/patients/999999999")
            .then()
            .statusCode(400);
    }
}