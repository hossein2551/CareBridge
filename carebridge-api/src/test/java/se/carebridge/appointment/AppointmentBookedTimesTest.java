
package se.carebridge.appointment;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import se.carebridge.patient.PatientTestCleanup;
import se.carebridge.healthcare.HealthcareUnitTestCleanup;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
public class AppointmentBookedTimesTest {

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
              "name": "Booked Times Test Unit",
              "city": "Kalmar",
              "organizationCode": "BT-%s"
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
              "firstName": "Booked%s",
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

  private Long createAppointment(
      Long patientId,
      Long unitId,
      String appointmentTime) {

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
  void shouldReturnBookedTimesForSelectedUnitAndDate() {
    Long firstUnit = createUnit();
    Long secondUnit = createUnit();

    Long firstPatient = createPatient(firstUnit);
    Long secondPatient = createPatient(secondUnit);

    LocalDate date = LocalDate.now().plusDays(30);

    createAppointment(
        firstPatient,
        firstUnit,
        date + "T09:00:00");

    createAppointment(
        firstPatient,
        firstUnit,
        date + "T14:30:00");

    createAppointment(
        secondPatient,
        secondUnit,
        date + "T11:00:00");

    given()
        .queryParam("healthcareUnitId", firstUnit)
        .queryParam("date", date.toString())
        .when()
        .get("/api/appointments/booked-times")
        .then()
        .statusCode(200)
        .body("$", contains("09:00", "14:30"));
  }

  @Test
  void shouldNotReturnCancelledAppointments() {
    Long unitId = createUnit();
    Long patientId = createPatient(unitId);

    LocalDate date = LocalDate.now().plusDays(31);

    Long appointmentId = createAppointment(
        patientId,
        unitId,
        date + "T10:00:00");

    given()
        .contentType("application/json")
        .body("\"CANCELLED\"")
        .when()
        .patch("/api/appointments/{id}/status", appointmentId)
        .then()
        .statusCode(200);

    given()
        .queryParam("healthcareUnitId", unitId)
        .queryParam("date", date.toString())
        .when()
        .get("/api/appointments/booked-times")
        .then()
        .statusCode(200)
        .body("$", empty());
  }

  @Test
  void shouldReturn404ForUnknownHealthcareUnit() {
    given()
        .queryParam("healthcareUnitId", 999999999L)
        .queryParam("date", LocalDate.now().plusDays(30).toString())
        .when()
        .get("/api/appointments/booked-times")
        .then()
        .statusCode(404);
  }

  @Test
  void shouldReturn400WhenDateIsMissing() {
    Long unitId = createUnit();

    given()
        .queryParam("healthcareUnitId", unitId)
        .when()
        .get("/api/appointments/booked-times")
        .then()
        .statusCode(400);
  }
}
