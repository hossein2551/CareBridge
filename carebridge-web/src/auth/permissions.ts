
import { keycloak } from "./keycloak";

export function hasRole(role: string): boolean {
  return keycloak.hasRealmRole(role);
}

export function canViewPatients(): boolean {
  return (
    hasRole("ADMIN") ||
    hasRole("DOCTOR") ||
    hasRole("NURSE")
  );
}

export function canManagePatients(): boolean {
  return hasRole("ADMIN") || hasRole("DOCTOR");
}

export function canDeletePatients(): boolean {
  return hasRole("ADMIN");
}
