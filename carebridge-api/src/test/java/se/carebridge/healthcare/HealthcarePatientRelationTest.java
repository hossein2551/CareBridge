
package se.carebridge.healthcare;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import se.carebridge.patient.PatientTestCleanup;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class HealthcarePatientRelationTest {

    @Inject
    PatientTestCleanup patientCleanup;

    @Inject
    HealthcareUnitTestCleanup unitCleanup;

    private Long patientId;
    private Long unitId;

    @AfterEach
    void cleanUp() {
        if (patientId != null) {
            patientCleanup.deletePatient(patientId);
        }

        if (unitId != null) {
            unitCleanup.deleteUnit(unitId);
        }
    }

    @Test
    public void shouldLinkPatientToHealthcareUnit() {

        String code = "REL-" + UUID.randomUUID();

        unitId = given()
            .contentType("application/json")
            .body("""
                {
                    "name": "CareBridge Relation Test",
                    "city": "Kalmar",
                    "organizationCode": "%s"
                }
                """.formatted(code))
        .when()
            .post("/api/healthcare-units")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");

        patientId = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Relation",
                    "lastName": "TestPatient",
                    "birthYear": 1995,
                    "healthcareUnitId": %d
                }
                """.formatted(unitId))
        .when()
            .post("/api/patients")
        .then()
            .statusCode(201)
            .body("healthcareUnitId", is(unitId.intValue()))
            .extract().jsonPath().getLong("id");

        given()
        .when()
            .get("/api/healthcare-units/" + unitId + "/patients")
        .then()
            .statusCode(200)
            .body("id", hasItem(patientId.intValue()))
            .body("firstName", hasItem("Relation"));
    }

    @Test
    public void shouldRejectUnknownHealthcareUnit() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Unknown",
                    "lastName": "Unit",
                    "birthYear": 1990,
                    "healthcareUnitId": 999999999
                }
                """)
        .when()
            .post("/api/patients")
        .then()
            .statusCode(404);
    }

    @Test
    public void shouldReturn404ForUnknownUnitPatients() {
        given()
        .when()
            .get("/api/healthcare-units/999999999/patients")
        .then()
            .statusCode(404);
    }
}
