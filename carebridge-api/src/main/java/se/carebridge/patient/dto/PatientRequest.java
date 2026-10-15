package se.carebridge.patient.dto;

import jakarta.validation.constraints.*;

public record PatientRequest(
    @NotBlank(message = "First name is required")
    @Size(max = 100)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    String lastName,

    @NotNull(message = "Birth year is required")
    @Min(1900)
    @Max(2100)
    Integer birthYear, Long healthcareUnitId
) {}