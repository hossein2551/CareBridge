package se.carebridge.patient.dto;

import java.util.List;

public record PatientPageResponse(
    List<PatientResponse> patients,
    int page,
    int size,
    long totalPatients,
    int totalPages
) {}