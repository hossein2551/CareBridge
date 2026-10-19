
package se.carebridge.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

@ApplicationScoped
public class CurrentUserService {

  @Inject
  SecurityIdentity identity;

  public String getUserId() {
    if (identity.isAnonymous()) {
      throw new ForbiddenException(
          "User must be authenticated");
    }

    return identity.getPrincipal().getName();
  }

  public boolean isAdmin() {
    return identity.hasRole("ADMIN");
  }
}
