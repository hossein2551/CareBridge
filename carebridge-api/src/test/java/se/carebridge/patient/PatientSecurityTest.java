
package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
public class PatientSecurityTest {

  @Test
  @TestSecurity(user = "test-admin", roles = { "ADMIN" })
  void adminShouldAccessPatientList() {
    given()
        .when()
        .get("/api/patients")
        .then()
        .statusCode(200)
        .body("$", isA(java.util.List.class));
  }

  @Test
  @TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
  void doctorWithoutAssignmentShouldSeeNoPatients() {
    given()
        .when()
        .get("/api/patients")
        .then()
        .statusCode(200)
        .body("$", hasSize(0));
  }

  @Test
  @TestSecurity(user = "test-doctor", roles = { "DOCTOR" })
  void doctorWithoutAssignmentCannotCreatePatient() {
    given()
        .contentType("application/json")
        .body("""
            {
                "firstName": "Security",
                "lastName": "Test",
                "birthYear": 1995,
                "healthcareUnitId": 1
            }
            """)
        .when()
        .post("/api/patients")
        .then()
        .statusCode(403);
  }

  @Test
  @TestSecurity(user = "test-nurse", roles = { "NURSE" })
  void nurseCannotCreatePatient() {
    given()
        .contentType("application/json")
        .body("""
            {
                "firstName": "Nurse",
                "lastName": "Test",
                "birthYear": 1995,
                "healthcareUnitId": 1
            }
            """)
        .when()
        .post("/api/patients")
        .then()
        .statusCode(403);
  }

  @Test
  @TestSecurity(user = "guest", roles = { "GUEST" })
  void guestCannotAccessPatients() {
    given()
        .when()
        .get("/api/patients")
        .then()
        .statusCode(403);
  }
}
