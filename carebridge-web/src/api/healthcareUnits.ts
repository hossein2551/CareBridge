
import { keycloak } from "../auth/keycloak";

const API_URL = "http://localhost:8080";

export interface HealthcareUnit {
  id: number;
  name: string;
  city: string;
  organizationCode: string;
  createdAt: string | null;
}

export async function getHealthcareUnits(): Promise<HealthcareUnit[]> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  if (!keycloak.token) {
    throw new Error("No access token available.");
  }

  const response = await fetch(`${API_URL}/api/healthcare-units`, {
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      Accept: "application/json",
    },
  });

  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 403) {
    throw new Error("Access denied (403).");
  }

  if (!response.ok) {
    throw new Error(`Failed to load healthcare units: ${response.status}`);
  }

  return (await response.json()) as HealthcareUnit[];
}
