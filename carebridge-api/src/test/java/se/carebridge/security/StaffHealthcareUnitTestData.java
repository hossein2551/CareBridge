
package se.carebridge.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class StaffHealthcareUnitTestData {

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String databaseUrl;

  @Transactional
  public Long createAssignment(
      String keycloakUserId,
      Long healthcareUnitId) {

    if (!databaseUrl.equals(
        "jdbc:postgresql://127.0.0.1:5432/carebridge_test_db")) {
      throw new IllegalStateException(
          "Test data may only be created in test database");
    }

    StaffHealthcareUnitEntity assignment = new StaffHealthcareUnitEntity();

    assignment.keycloakUserId = keycloakUserId;
    assignment.healthcareUnitId = healthcareUnitId;

    assignment.persistAndFlush();

    return assignment.id;
  }
}
