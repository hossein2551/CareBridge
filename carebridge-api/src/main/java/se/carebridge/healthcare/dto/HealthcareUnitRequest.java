package se.carebridge.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HealthcareUnitRequest(

    @NotBlank(message = "Name is required")
    @Size(max = 150)
    String name,

    @NotBlank(message = "City is required")
    @Size(max = 100)
    String city,

    @NotBlank(message = "Organization code is required")
    @Size(max = 50)
    String organizationCode

) {}