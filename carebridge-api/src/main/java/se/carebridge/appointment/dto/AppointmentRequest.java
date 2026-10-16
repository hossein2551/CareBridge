
package se.carebridge.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import java.time.LocalDateTime;

public record AppointmentRequest(

    @NotNull Long patientId,

    @NotNull Long healthcareUnitId,

    @NotNull @Future LocalDateTime appointmentTime

) {
}
