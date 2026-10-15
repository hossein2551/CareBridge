import { keycloak } from "../auth/keycloak";

const API_URL = "http://localhost:8080";

export interface Patient {
  id: number;
  firstName: string;
  lastName: string;
  birthYear: number;
  healthcareUnitId: number | null;
}

export async function getPatients(): Promise<Patient[]> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  if (!keycloak.token) {
    throw new Error("No access token available.");
  }

  const response = await fetch(`${API_URL}/api/patients`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${keycloak.token}`,
      Accept: "application/json",
    },
  });

  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 403) {
    throw new Error("Access denied (403). Check user roles.");
  }

  if (!response.ok) {
    throw new Error(`API request failed: ${response.status}`);
  }

  return (await response.json()) as Patient[];
}
export async function testDoctorDeletePermission(): Promise<number> {
  if (!keycloak.authenticated) {
    throw new Error("Not authenticated");
  }

  await keycloak.updateToken(30);

  const response = await fetch(
    "http://localhost:8080/api/patients/999999999",
    {
      method: "DELETE",
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
      },
    }
  );

  return response.status;
}



export interface PatientPage {
  patients: Patient[];
  page: number;
  size: number;
  totalPatients: number;
  totalPages: number;
}

export async function getPatientsPage(
  page: number = 0,
  size: number = 10
): Promise<PatientPage> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  if (!keycloak.token) {
    throw new Error("No access token available.");
  }

  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const response = await fetch(
    `http://localhost:8080/api/patients/page?${params}`,
    {
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        Accept: "application/json",
      },
    }
  );

  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 403) {
    throw new Error("Access denied (403).");
  }

  if (!response.ok) {
    throw new Error(`Failed to load patients: ${response.status}`);
  }

  return (await response.json()) as PatientPage;
}

export async function searchPatientsPage(
  name: string,
  page: number = 0,
  size: number = 10
): Promise<PatientPage> {
  if (!keycloak.authenticated) {
    throw new Error("You must be logged in.");
  }

  await keycloak.updateToken(30);

  if (!keycloak.token) {
    throw new Error("No access token available.");
  }

  const params = new URLSearchParams({
    name,
    page: String(page),
    size: String(size),
  });

  const response = await fetch(
    `${API_URL}/api/patients/search/page?${params.toString()}`,
    {
      method: "GET",
      headers: {
        Authorization: `Bearer ${keycloak.token}`,
        Accept: "application/json",
      },
    }
  );

  if (response.status === 401) {
    throw new Error("Authentication failed (401).");
  }

  if (response.status === 403) {
    throw new Error("Access denied (403).");
  }

  if (!response.ok) {
    throw new Error(
      `Failed to search patients: ${response.status}`
    );
  }

  return (await response.json()) as PatientPage;
}

