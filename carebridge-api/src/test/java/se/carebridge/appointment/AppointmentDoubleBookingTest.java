
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
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = { "ADMIN" })
public class AppointmentDoubleBookingTest {

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
              "name": "Double Booking Test Unit",
              "city": "Kalmar",
              "organizationCode": "DB-%s"
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
              "firstName": "Booking%s",
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

  private String futureTime() {
    return LocalDateTime.now()
        .plusDays(20)
        .withHour(10)
        .withMinute(0)
        .withSecond(0)
        .withNano(0)
        .toString();
  }

  private String requestBody(
      Long patientId,
      Long unitId,
      String appointmentTime) {

    return """
        {
          "patientId": %d,
          "healthcareUnitId": %d,
          "appointmentTime": "%s"
        }
        """.formatted(patientId, unitId, appointmentTime);
  }

  private Long bookAppointment(
      Long patientId,
      Long unitId,
      String appointmentTime) {

    Long id = given()
        .contentType("application/json")
        .body(requestBody(patientId, unitId, appointmentTime))
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

    appointmentIds.clear();
    patientIds.clear();
    unitIds.clear();
  }

  @Test
  void shouldRejectDoubleBookingAtSameUnitAndTime() {
    Long unitId = createUnit();
    Long firstPatient = createPatient(unitId);
    Long secondPatient = createPatient(unitId);

    String time = futureTime();

    bookAppointment(firstPatient, unitId, time);

    given()
        .contentType("application/json")
        .body(requestBody(secondPatient, unitId, time))
        .when()
        .post("/api/appointments")
        .then()
        .statusCode(409);
  }

  @Test
  void shouldAllowSameTimeAtDifferentUnits() {
    Long firstUnit = createUnit();
    Long secondUnit = createUnit();

    Long firstPatient = createPatient(firstUnit);
    Long secondPatient = createPatient(secondUnit);

    String time = futureTime();

    bookAppointment(firstPatient, firstUnit, time);
    bookAppointment(secondPatient, secondUnit, time);
  }

  @Test
  void shouldAllowBookingAfterCancellation() {
    Long unitId = createUnit();
    Long firstPatient = createPatient(unitId);
    Long secondPatient = createPatient(unitId);

    String time = futureTime();

    Long firstAppointment = bookAppointment(firstPatient, unitId, time);

    given()
        .contentType("application/json")
        .body("\"CANCELLED\"")
        .when()
        .patch("/api/appointments/{id}/status", firstAppointment)
        .then()
        .statusCode(200)
        .body("status", is("CANCELLED"));

    bookAppointment(secondPatient, unitId, time);
  }
}
