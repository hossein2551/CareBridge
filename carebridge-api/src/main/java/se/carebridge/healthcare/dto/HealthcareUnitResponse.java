package se.carebridge.healthcare.dto;

import java.time.OffsetDateTime;

public record HealthcareUnitResponse(
    Long id,
    String name,
    String city,
    String organizationCode,
    OffsetDateTime createdAt
) {}