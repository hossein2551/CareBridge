
package se.carebridge.security;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import se.carebridge.healthcare.HealthcareUnitTestCleanup;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class HealthcareAccessServiceTest {

  @Inject
  HealthcareAccessService accessService;

  @Inject
  StaffHealthcareUnitTestData testData;

  @Inject
  StaffHealthcareUnitTestCleanup assignmentCleanup;

  @Inject
  HealthcareUnitTestCleanup unitCleanup;

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String databaseUrl;

  private Long testAssignmentId;
  private Long testUnitId;

  @AfterEach
  void cleanUpTestData() {

    if (testAssignmentId != null) {
      assignmentCleanup.deleteAssignment(testAssignmentId);
      testAssignmentId = null;
    }

    if (testUnitId != null) {
      unitCleanup.deleteUnit(testUnitId);
      testUnitId = null;
    }
  }

  // TEST 1: Admin can access healthcare units

  @Test
  @TestSecurity(user = "test-admin", roles = { "ADMIN" })
  void adminShouldAccessAnyHealthcareUnit() {

    assertTrue(accessService.canAccessUnit(1L));
  }

  // TEST 2: Admin cannot access a null unit

  @Test
  @TestSecurity(user = "test-admin", roles = { "ADMIN" })
  void adminShouldNotAccessNullHealthcareUnit() {

    assertFalse(accessService.canAccessUnit(null));
  }

  // TEST 3: Doctor cannot access a null unit

  @Test
  @TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
  @OidcSecurity(claims = {
      @Claim(key = "sub", value = "test-doctor-null")
  })
  void doctorShouldNotAccessNullHealthcareUnit() {

    assertFalse(accessService.canAccessUnit(null));
  }

  // TEST 4: Doctor cannot access an unassigned unit

  @Test
  @TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
  @OidcSecurity(claims = {
      @Claim(key = "sub", value = "test-doctor-without-access")
  })
  void doctorShouldNotAccessUnassignedHealthcareUnit() {

    assertFalse(accessService.canAccessUnit(999999L));
  }

  // TEST 5: Doctor can access an assigned unit
  // and cannot access another unit

  @Test
  @TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
  @OidcSecurity(claims = {
      @Claim(key = "sub", value = "test-doctor-with-access")
  })
  void doctorShouldOnlyAccessAssignedHealthcareUnit() {

    // Verify that we are using the test database.
    assertEquals(
        "jdbc:postgresql://127.0.0.1:5432/carebridge_test_db",
        databaseUrl);

    // Create a fictional healthcare unit.
    String unique = UUID.randomUUID().toString();

    testUnitId = given()
        .contentType(MediaType.APPLICATION_JSON)
        .body("""
            {
              "name": "Security Test Unit",
              "city": "Kalmar",
              "organizationCode": "SEC-%s"
            }
            """.formatted(unique))
        .when()
        .post("/api/healthcare-units")
        .then()
        .statusCode(201)
        .extract()
        .jsonPath()
        .getLong("id");

    // Assign the healthcare unit to the doctor.
    testAssignmentId = testData.createAssignment(
        "test-doctor-with-access",
        testUnitId);

            assertTrue(
            accessService.hasAssignment(
                "test-doctor-with-access",
                testUnitId
            ),
            "Doctor must have access to the assigned unit"
        );

        assertFalse(
            accessService.hasAssignment(
                "test-doctor-with-access",
                999999L
            ),
            "Doctor must not have access to another unit"
        );
    }
}