package se.carebridge.healthcare;

import jakarta.inject.Inject;
import se.carebridge.patient.PatientService;
import se.carebridge.patient.dto.PatientResponse;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import se.carebridge.healthcare.dto.HealthcareUnitRequest;
import se.carebridge.healthcare.dto.HealthcareUnitResponse;

import java.net.URI;
import java.util.List;

@Path("/api/healthcare-units")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class HealthcareUnitResource {

    @Inject
    HealthcareUnitService healthcareUnitService;

    @GET
    public List<HealthcareUnitResponse> getAllUnits() {
        return healthcareUnitService.getAllUnits();
    }

    @GET
    @Path("/search")
    public List<HealthcareUnitResponse> searchByCity(
            @QueryParam("city") String city) {

        if (city == null || city.isBlank()) {
            throw new BadRequestException("City is required");
        }

        return healthcareUnitService.searchByCity(city);
    }

    @GET
    @Path("/{id}")
    public HealthcareUnitResponse getUnitById(@PathParam("id") Long id) {
        return healthcareUnitService.getUnitById(id);
    }

    @POST
    public Response createUnit(@Valid HealthcareUnitRequest request) {
        HealthcareUnitResponse saved = healthcareUnitService.createUnit(request);

        return Response.created(
            URI.create("/api/healthcare-units/" + saved.id())
        ).entity(saved).build();
    }

    @Inject
    PatientService patientService;

    @GET
    @Path("/{id}/patients")
    public List<PatientResponse> getPatientsByUnit(
            @PathParam("id") Long id) {
        return patientService.getPatientsByHealthcareUnit(id);
    }
}