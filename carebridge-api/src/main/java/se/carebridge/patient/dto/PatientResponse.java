package se.carebridge.patient.dto;

public record PatientResponse(
    Long id,
    String firstName,
    String lastName,
    Integer birthYear, Long healthcareUnitId
) {}