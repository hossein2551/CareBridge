
package se.carebridge.patient;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestSecurity(user = "test-admin", roles = {"ADMIN"})
public class PatientDeleteTest {

    @Test
    public void shouldDeletePatient() {
        Long id = given()
            .contentType("application/json")
            .body("""
                {
                    "firstName": "Delete",
                    "lastName": "Test",
                    "birthYear": 2000
                }
                """)
        .when()
            .post("/api/patients")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

        given()
        .when()
            .delete("/api/patients/" + id)
        .then()
            .statusCode(204);

        given()
        .when()
            .get("/api/patients/" + id)
        .then()
            .statusCode(404);
    }

    @Test
    public void shouldReturn404WhenDeletingUnknownPatient() {
        given()
        .when()
            .delete("/api/patients/999999999")
        .then()
            .statusCode(404);
    }
}
