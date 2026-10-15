package se.carebridge.patient;

import jakarta.inject.Inject;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import se.carebridge.patient.dto.PatientRequest;
import se.carebridge.patient.dto.PatientResponse;

@Path("/api/patients")
@RolesAllowed({"ADMIN", "DOCTOR", "NURSE"})
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PatientResource {

    @Inject
    PatientService patientService;

    @GET
    public List<PatientResponse> getAllPatients() {
        return patientService.getAllPatients();
    }

    @GET
    @Path("/{id}")
    public PatientResponse getPatientById(@PathParam("id") Long id) {
        return patientService.getPatientById(id);
    }

    @POST
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public Response createPatient(@Valid PatientRequest request) {
        PatientResponse saved = patientService.createPatient(request);

        return Response.created(
            URI.create("/api/patients/" + saved.id())
        ).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed({"ADMIN", "DOCTOR"})
    public PatientResponse updatePatient(
            @PathParam("id") Long id,
            @Valid PatientRequest request) {
        return patientService.updatePatient(id, request);
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed("ADMIN")
    public Response deletePatient(@PathParam("id") Long id) {
        boolean deleted = patientService.deletePatient(id);

        if (!deleted) {
            throw new NotFoundException("Patient not found");
        }

        return Response.noContent().build();
    }

    @GET
    @Path("/search")
    public List<PatientResponse> searchPatients(
            @QueryParam("name") String name) {

        if (name == null || name.isBlank()) {
            throw new BadRequestException("Search name is required");
        }

        return patientService.searchPatients(name);
    }

    @GET
    @Path("/page")
    public se.carebridge.patient.dto.PatientPageResponse getPatientsPage(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        return patientService.getPatientsPage(page, size);
    }

    @GET
    @Path("/search/page")
    public se.carebridge.patient.dto.PatientPageResponse searchPatientsPage(
            @QueryParam("name") @DefaultValue("") String name,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size) {

        return patientService.searchPatientsPage(name, page, size);
    }

}