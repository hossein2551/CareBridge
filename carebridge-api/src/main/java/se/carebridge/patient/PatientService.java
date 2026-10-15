package se.carebridge.patient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import se.carebridge.healthcare.HealthcareUnitEntity;
import se.carebridge.patient.dto.PatientRequest;
import se.carebridge.patient.dto.PatientResponse;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class PatientService {

    private PatientResponse toResponse(PatientEntity entity) {
        return new PatientResponse(
            entity.id,
            entity.firstName,
            entity.lastName,
            entity.birthYear,
            entity.healthcareUnit == null
                ? null
                : entity.healthcareUnit.id
        );
    }

    public List<PatientResponse> getAllPatients() {
        return PatientEntity.<PatientEntity>listAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public PatientResponse getPatientById(Long id) {
        PatientEntity entity = PatientEntity.findById(id);

        if (entity == null) {
            throw new NotFoundException("Patient not found");
        }

        return toResponse(entity);
    }

    private HealthcareUnitEntity findHealthcareUnit(Long id) {
        if (id == null) {
            return null;
        }

        HealthcareUnitEntity unit = HealthcareUnitEntity.findById(id);

        if (unit == null) {
            throw new NotFoundException("Healthcare unit not found");
        }

        return unit;
    }

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        PatientEntity entity = new PatientEntity(
            request.firstName(),
            request.lastName(),
            request.birthYear()
        );

        entity.healthcareUnit = findHealthcareUnit(
            request.healthcareUnitId()
        );

        entity.persist();

        return toResponse(entity);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        PatientEntity entity = PatientEntity.findById(id);

        if (entity == null) {
            throw new NotFoundException("Patient not found");
        }

        HealthcareUnitEntity unit = findHealthcareUnit(
            request.healthcareUnitId()
        );

        entity.firstName = request.firstName();
        entity.lastName = request.lastName();
        entity.birthYear = request.birthYear();
        entity.healthcareUnit = unit;

        return toResponse(entity);
    }

    @Transactional
    public boolean deletePatient(Long id) {
        return PatientEntity.deleteById(id);
    }

    public List<PatientResponse> searchPatients(String name) {
        String search = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";

        return PatientEntity.<PatientEntity>list(
            "LOWER(firstName) LIKE ?1 OR LOWER(lastName) LIKE ?1",
            search
        )
        .stream()
        .map(this::toResponse)
        .toList();
    }

    public List<PatientResponse> getPatientsByHealthcareUnit(Long unitId) {
        findHealthcareUnit(unitId);

        return PatientEntity.<PatientEntity>list(
            "healthcareUnit.id", unitId
        )
        .stream()
        .map(this::toResponse)
        .toList();
    }

    public se.carebridge.patient.dto.PatientPageResponse getPatientsPage(
            int page, int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new jakarta.ws.rs.BadRequestException(
                "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        var query = PatientEntity.<PatientEntity>findAll(io.quarkus.panache.common.Sort.by("id"))
            .page(io.quarkus.panache.common.Page.of(page, size));

        long total = query.count();

        var patients = query.list()
            .stream()
            .map(this::toResponse)
            .toList();

        int totalPages = (int) ((total + size - 1) / size);

        return new se.carebridge.patient.dto.PatientPageResponse(
            patients,
            page,
            size,
            total,
            totalPages
        );
    }

public se.carebridge.patient.dto.PatientPageResponse
searchPatientsPage(String name, int page, int size) {

    if (page < 0 || size < 1 || size > 100) {
        throw new jakarta.ws.rs.BadRequestException(
            "Page must be >= 0 and size must be between 1 and 100"
        );
    }

    if (name == null || name.isBlank()) {
        return getPatientsPage(page, size);
    }

    String search = "%" +
        name.trim().toLowerCase(java.util.Locale.ROOT) + "%";

    var query = PatientEntity.<PatientEntity>find(
        "LOWER(firstName) LIKE ?1 OR LOWER(lastName) LIKE ?1",
        io.quarkus.panache.common.Sort.by("id"),
        search
    ).page(io.quarkus.panache.common.Page.of(page, size));

    long total = query.count();

    var patients = query.list()
        .stream()
        .map(this::toResponse)
        .toList();

    int totalPages = (int) ((total + size - 1) / size);

    return new se.carebridge.patient.dto.PatientPageResponse(
        patients,
        page,
        size,
        total,
        totalPages
    );
}

}