
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
public class AppointmentResourceTest {

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
              "name": "Appointment Test Unit",
              "city": "Kalmar",
              "organizationCode": "APPT-%s"
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
              "firstName": "Appointment%s",
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

  private Long createAppointment(Long patientId, Long unitId) {
    String appointmentTime = LocalDateTime.now()
        .plusDays(14)
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
        .body("status", is("SCHEDULED"))
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
  void shouldCreateAppointment() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);

    Long appointmentId = createAppointment(patientId, unitId);

    given()
        .when()
        .get("/api/appointments/{id}", appointmentId)
        .then()
        .statusCode(200)
        .body("id", is(appointmentId.intValue()))
        .body("status", is("SCHEDULED"));
  }

  @Test
  void shouldCompleteAppointment() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);
    Long appointmentId = createAppointment(patientId, unitId);

    given()
        .contentType("application/json")
        .body("\"COMPLETED\"")
        .when()
        .patch("/api/appointments/{id}/status", appointmentId)
        .then()
        .statusCode(200)
        .body("status", is("COMPLETED"));
  }

  @Test
  void shouldCancelAppointment() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);
    Long appointmentId = createAppointment(patientId, unitId);

    given()
        .contentType("application/json")
        .body("\"CANCELLED\"")
        .when()
        .patch("/api/appointments/{id}/status", appointmentId)
        .then()
        .statusCode(200)
        .body("status", is("CANCELLED"));
  }

  @Test
  void shouldRejectSecondStatusChange() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);
    Long appointmentId = createAppointment(patientId, unitId);

    given()
        .contentType("application/json")
        .body("\"COMPLETED\"")
        .when()
        .patch("/api/appointments/{id}/status", appointmentId)
        .then()
        .statusCode(200);

    given()
        .contentType("application/json")
        .body("\"CANCELLED\"")
        .when()
        .patch("/api/appointments/{id}/status", appointmentId)
        .then()
        .statusCode(400);
  }

  @Test
  @TestSecurity(user = "test-nurse", roles = { "NURSE" })
  void nurseShouldNotCreateAppointment() {
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .post("/api/appointments")
        .then()
        .statusCode(403);
  }

  @Test
  @TestSecurity(user = "test-nurse", roles = { "NURSE" })
  void nurseShouldNotChangeAppointmentStatus() {
    given()
        .contentType("application/json")
        .body("\"COMPLETED\"")
        .when()
        .patch("/api/appointments/999999/status")
        .then()
        .statusCode(403);
  }
}
