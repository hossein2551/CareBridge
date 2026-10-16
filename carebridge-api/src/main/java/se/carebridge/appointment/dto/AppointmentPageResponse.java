
package se.carebridge.appointment.dto;

import java.util.List;

public record AppointmentPageResponse(
    List<AppointmentResponse> appointments,
    int page,
    int size,
    long totalAppointments,
    int totalPages) {
}
