
package se.carebridge.appointment.dto;

import se.carebridge.appointment.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(
    Long id,
    Long patientId,
    Long healthcareUnitId,
    LocalDateTime appointmentTime,
    AppointmentStatus status) {
}
