
package se.carebridge.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@ApplicationScoped
public class HealthcareAccessService {

    @Inject
    SecurityIdentity identity;

    @Inject
    JsonWebToken accessToken;

    public boolean isAdmin() {
        return !identity.isAnonymous()
            && identity.hasRole("ADMIN");
    }

    public List<Long> getAllowedUnitIds() {
        if (identity.isAnonymous()) {
            return List.of();
        }

        String userId = accessToken.getSubject();

        if (userId == null || userId.isBlank()) {
            return List.of();
        }

        return StaffHealthcareUnitEntity
            .<StaffHealthcareUnitEntity>list(
                "keycloakUserId", userId
            )
            .stream()
            .map(assignment -> assignment.healthcareUnitId)
            .toList();
    }

    public boolean canAccessUnit(Long healthcareUnitId) {

        if (identity.isAnonymous() || healthcareUnitId == null) {
            return false;
        }

        if (isAdmin()) {
            return true;
        }

        String userId = accessToken.getSubject();

        return hasAssignment(userId, healthcareUnitId);
    }

    boolean hasAssignment(String userId, Long healthcareUnitId) {

        if (userId == null || userId.isBlank()
                || healthcareUnitId == null) {
            return false;
        }

        return StaffHealthcareUnitEntity.count(
            "keycloakUserId = ?1 and healthcareUnitId = ?2",
            userId,
            healthcareUnitId
        ) > 0;
    }

    public void requireUnitAccess(Long healthcareUnitId) {

        if (!canAccessUnit(healthcareUnitId)) {
            throw new ForbiddenException(
                "You do not have access to this healthcare unit"
            );
        }
    }
}
