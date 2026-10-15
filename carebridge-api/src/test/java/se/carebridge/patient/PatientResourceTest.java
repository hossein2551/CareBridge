package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestSecurity(user = "test-doctor", roles = {"DOCTOR"})
public class PatientResourceTest {

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
    public void shouldGetAllPatients() {
        given()
        .when()
            .get("/api/patients")
        .then()
            .statusCode(200)
            .body("$", isA(java.util.List.class));
    }

    @Test
    public void shouldReturn404ForUnknownPatient() {
        given()
        .when()
            .get("/api/patients/999999999")
        .then()
            .statusCode(404);
    }

    @Test
    public void shouldCreatePatient() {
        Response response = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Test",
                    "lastName": "Patient",
                    "birthYear": 1999
                }
                """)
        .when()
            .post("/api/patients");

        response.then()
            .statusCode(201)
            .body("firstName", is("Test"))
            .body("lastName", is("Patient"))
            .body("id", notNullValue());

        Long id = response.jsonPath().getLong("id");
        createdPatientId = id;

        given()
        .when()
            .get("/api/patients/" + id)
        .then()
            .statusCode(200)
            .body("firstName", is("Test"));
    }
}