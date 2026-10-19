
package se.carebridge.appointment;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import se.carebridge.patient.PatientTestCleanup;
import se.carebridge.healthcare.HealthcareUnitTestCleanup;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = { "ADMIN" })
public class AppointmentPatientHistoryTest {

  @Inject
  AppointmentTestCleanup appointmentCleanup;

  @Inject
  PatientTestCleanup patientCleanup;

  @Inject
  HealthcareUnitTestCleanup unitCleanup;

  private final List<Long> appointmentIds = new ArrayList<>();
  private final List<Long> patientIds = new ArrayList<>();
  private final List<Long> unitIds = new ArrayList<>();

  private Long createUnit() {
    String unique = UUID.randomUUID().toString();

    Long id = given()
        .contentType("application/json")
        .body("""
            {
              "name": "History Test Unit",
              "city": "Kalmar",
              "organizationCode": "HIST-%s"
            }
            """.formatted(unique))
        .when()
        .post("/api/healthcare-units")
        .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");

    unitIds.add(id);
    return id;
  }

  private Long createPatient(Long unitId) {
    String unique = UUID.randomUUID()
        .toString().replace("-", "");

    Long id = given()
        .contentType("application/json")
        .body("""
            {
              "firstName": "History%s",
              "lastName": "Test",
              "birthYear": 1995,
              "healthcareUnitId": %d
            }
            """.formatted(unique, unitId))
        .when()
        .post("/api/patients")
        .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");

    patientIds.add(id);
    return id;
  }

  private Long createAppointment(Long patientId, Long unitId, int daysAhead) {
    String appointmentTime = LocalDateTime.now()
        .plusDays(daysAhead)
        .withNano(0)
        .toString();

    Long id = given()
        .contentType("application/json")
        .body("""
            {
              "patientId": %d,
              "healthcareUnitId": %d,
              "appointmentTime": "%s"
            }
            """.formatted(patientId, unitId, appointmentTime))
        .when()
        .post("/api/appointments")
        .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");

    appointmentIds.add(id);
    return id;
  }

  @AfterEach
  void cleanUp() {
    for (Long id : appointmentIds) {
      appointmentCleanup.deleteAppointment(id);
    }

    for (Long id : patientIds) {
      patientCleanup.deletePatient(id);
    }

    for (Long id : unitIds) {
      unitCleanup.deleteUnit(id);
    }
  }

  @Test
  void shouldReturnOnlySelectedPatientsAppointments() {
    Long unitId = createUnit();

    Long patient1 = createPatient(unitId);
    Long patient2 = createPatient(unitId);

    Long appointment1 = createAppointment(patient1, unitId, 14);
    Long appointment2 = createAppointment(patient2, unitId, 15);

    given()
        .when()
        .get("/api/appointments/patient/{patientId}", patient1)
        .then()
        .statusCode(200)
        .body("size()", is(1))
        .body("[0].id", is(appointment1.intValue()))
        .body("[0].patientId", is(patient1.intValue()))
        .body("id", not(hasItem(appointment2.intValue())));
  }

  @Test
  void shouldReturnEmptyListWhenPatientHasNoAppointments() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);

    given()
        .when()
        .get("/api/appointments/patient/{patientId}", patientId)
        .then()
        .statusCode(200)
        .body("size()", is(0));
  }

  @Test
  void shouldRejectInvalidPatientId() {
    given()
        .when()
        .get("/api/appointments/patient/{patientId}", 0)
        .then()
        .statusCode(400);
  }

  @Test
  void shouldReturn404WhenPatientDoesNotExist() {
    given()
        .when()
        .get("/api/appointments/patient/{patientId}", Long.MAX_VALUE)
        .then()
        .statusCode(404);
  }
  
  @Test
  @TestSecurity(user = "unauthorized-user", roles = {"GUEST"})
  void guestShouldNotAccessPatientHistory() {
    given()
        .when()
        .get("/api/appointments/patient/1")
        .then()
        .statusCode(403);
  }

 @Test
@TestSecurity(user = "", roles = {})
void anonymousUserShouldNotAccessPatientHistory() {
    given()
        .when()
        .get("/api/appointments/patient/1")
        .then()
        .statusCode(401);
}

}
