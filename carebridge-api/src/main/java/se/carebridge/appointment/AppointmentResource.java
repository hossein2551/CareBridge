
package se.carebridge.appointment;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import se.carebridge.appointment.dto.AppointmentRequest;
import se.carebridge.appointment.dto.AppointmentResponse;
import se.carebridge.appointment.dto.AppointmentPageResponse;

import java.time.LocalDate;
import java.util.List;

@Path("/api/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ "ADMIN", "DOCTOR", "NURSE" })
public class AppointmentResource {

  @Inject
  AppointmentService appointmentService;

  // Get all appointments
  @GET
  public List<AppointmentResponse> getAllAppointments() {
    return appointmentService.getAllAppointments();
  }

  // Search, filter and paginate appointments
  @GET
  @Path("/filter/page")
  public AppointmentPageResponse filterAppointmentsPage(
      @QueryParam("patientName") @DefaultValue("") String patientName,
      @QueryParam("healthcareUnitId") Long healthcareUnitId,
      @QueryParam("status") AppointmentStatus status,
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size) {

    return appointmentService.filterAppointmentsPage(
        patientName,
        healthcareUnitId,
        status,
        page,
        size);
  }

  // Get booked times for a healthcare unit on a given date
  @GET
  @Path("/booked-times")
  public List<String> getBookedTimes(
      @QueryParam("healthcareUnitId") Long healthcareUnitId,
      @QueryParam("date") LocalDate date) {

    if (healthcareUnitId == null || date == null) {
      throw new BadRequestException(
          "healthcareUnitId and date are required");
    }

    return appointmentService.getBookedTimes(
        healthcareUnitId,
        date);
  }

  // Get appointment by ID
  @GET
  @Path("/{id}")
  public AppointmentResponse getAppointmentById(
      @PathParam("id") Long id) {

    return appointmentService.getAppointmentById(id);
  }

  // Create new appointment
  @POST
  @RolesAllowed({ "ADMIN", "DOCTOR" })
  public Response createAppointment(
      @Valid AppointmentRequest request) {

    AppointmentResponse appointment = appointmentService.createAppointment(request);

    return Response.status(Response.Status.CREATED)
        .entity(appointment)
        .build();
  }

  // Update appointment status
  @PATCH
  @Path("/{id}/status")
  @RolesAllowed({ "ADMIN", "DOCTOR" })
  public AppointmentResponse updateAppointmentStatus(
      @PathParam("id") Long id,
      AppointmentStatus newStatus) {

    return appointmentService.updateAppointmentStatus(
        id,
        newStatus);
  }
}
