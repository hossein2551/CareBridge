
package se.carebridge.appointment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AppointmentTestCleanup {

  @Inject
  @ConfigProperty(name = "quarkus.datasource.jdbc.url")
  String databaseUrl;

  @Transactional
  public void deleteAppointment(Long id) {

    if (!databaseUrl.matches(
        "^jdbc:postgresql://(127\\.0\\.0\\.1|localhost):5432/carebridge_test_db$")) {
      throw new IllegalStateException(
          "Cleanup is only allowed in carebridge_test_db");
    }

    AppointmentEntity.deleteById(id);
  }
}
