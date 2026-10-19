package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import io.quarkus.test.security.TestSecurity;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientValidationTest {

    @Test
    public void shouldRejectEmptyFirstName() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "",
                    "lastName": "Karlsson",
                    "birthYear": 1998
                }
                """)
        .when()
            .post("/api/patients")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectInvalidBirthYear() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Emma",
                    "lastName": "Karlsson",
                    "birthYear": 1800
                }
                """)
        .when()
            .post("/api/patients")
        .then()
            .statusCode(400);
    }

    @Test
    public void shouldRejectMissingLastName() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Emma",
                    "birthYear": 1998
                }
                """)
        .when()
            .post("/api/patients")
        .then()
            .statusCode(400);
    }
}