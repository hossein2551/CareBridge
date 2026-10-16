
package se.carebridge.appointment;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import se.carebridge.appointment.dto.AppointmentRequest;
import se.carebridge.appointment.dto.AppointmentResponse;

import java.util.List;

@Path("/api/appointments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ "ADMIN", "DOCTOR", "NURSE" })
public class AppointmentResource {

  @Inject
  AppointmentService appointmentService;

  @GET
  public List<AppointmentResponse> getAllAppointments() {
    return appointmentService.getAllAppointments();
  }

  @GET
  @Path("/{id}")
  public AppointmentResponse getAppointmentById(
      @PathParam("id") Long id) {

    return appointmentService.getAppointmentById(id);
  }

  @POST
  @RolesAllowed({ "ADMIN", "DOCTOR" })
  public Response createAppointment(
      @Valid AppointmentRequest request) {

    AppointmentResponse appointment = appointmentService.createAppointment(request);

    return Response.status(Response.Status.CREATED)
        .entity(appointment)
        .build();
  }

  @PATCH
  @Path("/{id}/status")
  @RolesAllowed({ "ADMIN", "DOCTOR" })
  public AppointmentResponse updateAppointmentStatus(
      @PathParam("id") Long id,
      AppointmentStatus newStatus) {

    return appointmentService.updateAppointmentStatus(
        id, newStatus);
  }

}
