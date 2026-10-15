package se.carebridge.healthcare;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.inject.Inject;

@ApplicationScoped
public class HealthcareUnitTestCleanup {

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String databaseUrl;

    @Transactional
    public void deleteUnit(Long id) {
        if (!databaseUrl.equals(
            "jdbc:postgresql://127.0.0.1:5432/carebridge_test_db")) {
            throw new IllegalStateException(
                "Cleanup only allowed in test database"
            );
        }

        HealthcareUnitEntity.deleteById(id);
    }
}