
package se.carebridge.patient;

import se.carebridge.healthcare.HealthcareUnitTestCleanup;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
public class PatientFilterTest {

  @Inject
  PatientTestCleanup cleanup;
  @Inject
  HealthcareUnitTestCleanup unitCleanup;

  private Long createdUnitId;

  private final List<Long> createdIds = new ArrayList<>();

  private Long createPatient(String firstName, Long unitId) {
    String body = """
        {
            "firstName": "%s",
            "lastName": "FilterTest",
            "birthYear": 1995,
            "healthcareUnitId": %s
        }
        """.formatted(
        firstName,
        unitId == null ? "null" : unitId.toString());

    Long id = given()
        .contentType("application/json")
        .body(body)
        .when()
        .post("/api/patients")
        .then()
        .statusCode(201)
        .extract().jsonPath().getLong("id");

    createdIds.add(id);
    return id;
  }

  @AfterEach
  void cleanUp() {
    for (Long id : createdIds) {
      cleanup.deletePatient(id);
    }
    createdIds.clear();

    if (createdUnitId != null) {
      unitCleanup.deleteUnit(createdUnitId);
      createdUnitId = null;
    }
  }

  @Test
  void shouldFilterByHealthcareUnit() {
    String unique = UUID.randomUUID().toString();

    String organizationCode = "FILTER-" + unique;

    createdUnitId = given()
        .contentType("application/json")
        .body("""
            {
                "name": "CareBridge Filter Test Unit",
                "city": "Kalmar",
                "organizationCode": "%s"
            }
            """.formatted(organizationCode))
        .when()
        .post("/api/healthcare-units")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    String name = "UnitFilter" +
        unique.replace("-", "");

    Long matchingId = createPatient(name, createdUnitId);
    createPatient(name, null);

    given()
        .queryParam("name", name)
        .queryParam("healthcareUnitId", createdUnitId)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/patients/filter/page")
        .then()
        .statusCode(200)
        .body("totalPatients", is(1))
        .body("patients.id", hasItem(matchingId.intValue()));
  }

  @Test
  void shouldFilterByNameWithoutUnit() {
    String name = "NameFilter" +
        UUID.randomUUID().toString().replace("-", "");

    createPatient(name, null);

    given()
        .queryParam("name", name)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/patients/filter/page")
        .then()
        .statusCode(200)
        .body("totalPatients", is(1))
        .body("patients.firstName", hasItem(name));
  }

  @Test
  void shouldRejectInvalidPageSize() {
    given()
        .queryParam("page", 0)
        .queryParam("size", 0)
        .when()
        .get("/api/patients/filter/page")
        .then()
        .statusCode(400);
  }

  @Test
  void shouldRejectUnknownHealthcareUnit() {
    given()
        .queryParam("healthcareUnitId", 999999999)
        .queryParam("page", 0)
        .queryParam("size", 10)
        .when()
        .get("/api/patients/filter/page")
        .then()
        .statusCode(404);
  }
}
