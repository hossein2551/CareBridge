
import Keycloak from "keycloak-js";

export const keycloak = new Keycloak({
  url: "http://localhost:8180",
  realm: "carebridge",
  clientId: "carebridge-web",
});

export async function initializeAuth(): Promise<boolean> {
  return keycloak.init({
    onLoad: "check-sso",
    pkceMethod: "S256",
    checkLoginIframe: false,
  });
}
