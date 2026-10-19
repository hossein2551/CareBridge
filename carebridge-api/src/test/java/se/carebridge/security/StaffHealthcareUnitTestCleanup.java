
package se.carebridge.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class StaffHealthcareUnitTestCleanup {

  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String databaseUrl;

  @Transactional
  public void deleteAssignment(Long id) {

    if (!databaseUrl.equals(
        "jdbc:postgresql://127.0.0.1:5432/carebridge_test_db")) {
      throw new IllegalStateException(
          "Cleanup only allowed in test database");
    }

    StaffHealthcareUnitEntity.deleteById(id);
  }
}
