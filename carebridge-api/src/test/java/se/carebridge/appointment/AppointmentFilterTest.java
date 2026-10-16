
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
@TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
public class AppointmentFilterTest {

  @Inject
  AppointmentTestCleanup appointmentCleanup;

  @Inject
  PatientTestCleanup patientCleanup;

  @Inject
  HealthcareUnitTestCleanup unitCleanup;

  private final List<Long> appointmentIds = new ArrayList<>();
  private final List<Long> patientIds = new ArrayList<>();
  private final List<Long> unitIds = new ArrayList<>();

  private String uniqueName() {
    return "Filter" +
        UUID.randomUUID().toString().replace("-", "");
  }

  private Long createUnit() {
    String unique = UUID.randomUUID().toString();

    Long id = given()
        .contentType("application/json")
        .body("""
            {
              "name": "Appointment Filter Test Unit",
              "city": "Kalmar",
              "organizationCode": "FILTER-%s"
            }
            """.formatted(unique))
        .when()
        .post("/api/healthcare-units")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    unitIds.add(id);
    return id;
  }

  private Long createPatient(String firstName, Long unitId) {
    Long id = given()
        .contentType("application/json")
        .body("""
            {
              "firstName": "%s",
              "lastName": "AppointmentFilterTest",
              "birthYear": 1995,
              "healthcareUnitId": %d
            }
            """.formatted(firstName, unitId))
        .when()
        .post("/api/patients")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    patientIds.add(id);
    return id;
  }

  private Long createAppointment(
      Long patientId,
      Long unitId,
      int daysAhead) {

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
            """.formatted(
            patientId,
            unitId,
            appointmentTime))
        .when()
        .post("/api/appointments")
        .then()
        .statusCode(201)
        .body("status", is("SCHEDULED"))
        .extract()
        .jsonPath()
        .getLong("id");

    appointmentIds.add(id);
    return id;
  }

  private void updateStatus(Long id, String status) {
    given()
        .contentType("application/json")
        .body("\"" + status + "\"")
        .when()
        .patch("/api/appointments/{id}/status", id)
        .then()
        .statusCode(200)
        .body("status", is(status));
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

  // Test 1: Search by patient name
  @Test
  void shouldFilterByPatientName() {
    Long unitId = createUnit();
    String name = uniqueName();

    Long matchingPatient = createPatient(name, unitId);
    Long otherPatient = createPatient(uniqueName(), unitId);

    Long matchingAppointment = createAppointment(matchingPatient, unitId, 14);

    createAppointment(otherPatient, unitId, 15);

    given()
        .queryParam("patientName", name)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("totalAppointments", is(1))
        .body(
            "appointments.id",
            hasItem(matchingAppointment.intValue()));
  }

  // Test 2: Filter by healthcare unit
  @Test
  void shouldFilterByHealthcareUnit() {
    Long unitOne = createUnit();
    Long unitTwo = createUnit();

    String name = uniqueName();

    Long patientOne = createPatient(name, unitOne);
    Long patientTwo = createPatient(name, unitTwo);

    Long matchingAppointment = createAppointment(patientOne, unitOne, 14);

    createAppointment(patientTwo, unitTwo, 15);

    given()
        .queryParam("patientName", name)
        .queryParam("healthcareUnitId", unitOne)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("totalAppointments", is(1))
        .body(
            "appointments.id",
            hasItem(matchingAppointment.intValue()));
  }

  // Test 3: Filter by appointment status
  @Test
  void shouldFilterByStatus() {
    Long unitId = createUnit();
    String name = uniqueName();

    Long patientId = createPatient(name, unitId);

    Long scheduledId = createAppointment(patientId, unitId, 14);

    Long completedId = createAppointment(patientId, unitId, 15);

    updateStatus(completedId, "COMPLETED");

    given()
        .queryParam("patientName", name)
        .queryParam("status", "COMPLETED")
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("totalAppointments", is(1))
        .body(
            "appointments.id",
            hasItem(completedId.intValue()))
        .body(
            "appointments.id",
            not(hasItem(scheduledId.intValue())));
  }

  // Test 4: Combine name, unit and status filters
  @Test
  void shouldCombineMultipleFilters() {
    Long unitOne = createUnit();
    Long unitTwo = createUnit();

    String name = uniqueName();

    Long patientOne = createPatient(name, unitOne);
    Long patientTwo = createPatient(name, unitTwo);

    Long matchingId = createAppointment(patientOne, unitOne, 14);

    Long otherStatusId = createAppointment(patientOne, unitOne, 15);

    Long otherUnitId = createAppointment(patientTwo, unitTwo, 16);

    updateStatus(matchingId, "CANCELLED");
    updateStatus(otherUnitId, "CANCELLED");

    given()
        .queryParam("patientName", name)
        .queryParam("healthcareUnitId", unitOne)
        .queryParam("status", "CANCELLED")
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("totalAppointments", is(1))
        .body(
            "appointments.id",
            hasItem(matchingId.intValue()))
        .body(
            "appointments.id",
            not(hasItem(otherStatusId.intValue())))
        .body(
            "appointments.id",
            not(hasItem(otherUnitId.intValue())));
  }

  // Test 5: Server-side pagination
  @Test
  void shouldPaginateAppointments() {
    Long unitId = createUnit();
    String name = uniqueName();

    Long patientId = createPatient(name, unitId);

    createAppointment(patientId, unitId, 14);
    createAppointment(patientId, unitId, 15);
    createAppointment(patientId, unitId, 16);

    given()
        .queryParam("patientName", name)
        .queryParam("page", 0)
        .queryParam("size", 2)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("page", is(0))
        .body("size", is(2))
        .body("totalAppointments", is(3))
        .body("totalPages", is(2))
        .body("appointments.size()", is(2));

    given()
        .queryParam("patientName", name)
        .queryParam("page", 1)
        .queryParam("size", 2)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(200)
        .body("page", is(1))
        .body("totalAppointments", is(3))
        .body("totalPages", is(2))
        .body("appointments.size()", is(1));
  }

  // Test 6: Reject invalid pagination parameters
  @Test
  void shouldRejectInvalidPagination() {
    given()
        .queryParam("page", -1)
        .queryParam("size", 10)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(400);

    given()
        .queryParam("page", 0)
        .queryParam("size", 0)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(400);

    given()
        .queryParam("page", 0)
        .queryParam("size", 101)
        .when()
        .get("/api/appointments/filter/page")
        .then()
        .statusCode(400);
  }
}
