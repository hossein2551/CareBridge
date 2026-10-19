
package se.carebridge.patient;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import se.carebridge.healthcare.HealthcareUnitEntity;
import se.carebridge.patient.dto.PatientPageResponse;
import se.carebridge.patient.dto.PatientRequest;
import se.carebridge.patient.dto.PatientResponse;
import se.carebridge.security.HealthcareAccessService;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class PatientService {

    @Inject
    HealthcareAccessService accessService;

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

    private void requirePatientAccess(PatientEntity patient) {
        Long unitId = patient.healthcareUnit == null
            ? null
            : patient.healthcareUnit.id;

        if (unitId == null && accessService.isAdmin()) {
            return;
        }

        accessService.requireUnitAccess(unitId);
    }

    private void requireTargetUnitAccess(Long unitId) {
        if (unitId == null && accessService.isAdmin()) {
            return;
        }

        accessService.requireUnitAccess(unitId);
    }

    private PanacheQuery<PatientEntity> findVisiblePatients(
            String name,
            Long healthcareUnitId) {

        StringBuilder query = new StringBuilder("1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        String search = name == null
            ? ""
            : name.trim().toLowerCase(Locale.ROOT);

        if (!search.isEmpty()) {
            query.append(
                " AND (LOWER(firstName) LIKE :name " +
                "OR LOWER(lastName) LIKE :name)"
            );
            parameters.put("name", "%" + search + "%");
        }

        if (healthcareUnitId != null) {
            findHealthcareUnit(healthcareUnitId);

            if (!accessService.isAdmin()) {
                accessService.requireUnitAccess(healthcareUnitId);
            }

            query.append(" AND healthcareUnit.id = :unitId");
            parameters.put("unitId", healthcareUnitId);
        }

        if (!accessService.isAdmin()) {
            List<Long> allowedIds = accessService.getAllowedUnitIds();

            if (allowedIds.isEmpty()) {
                query.append(" AND 1 = 0");
            } else {
                query.append(
                    " AND healthcareUnit.id IN :allowedUnitIds"
                );
                parameters.put("allowedUnitIds", allowedIds);
            }
        }

        return PatientEntity.<PatientEntity>find(
            query.toString(),
            Sort.by("id"),
            parameters
        );
    }

    public List<PatientResponse> getAllPatients() {
        return findVisiblePatients(null, null)
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public PatientResponse getPatientById(Long id) {
        PatientEntity entity = PatientEntity.findById(id);

        if (entity == null) {
            throw new NotFoundException("Patient not found");
        }

        requirePatientAccess(entity);

        return toResponse(entity);
    }

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        requireTargetUnitAccess(request.healthcareUnitId());

        HealthcareUnitEntity unit = findHealthcareUnit(
            request.healthcareUnitId()
        );

        PatientEntity entity = new PatientEntity(
            request.firstName(),
            request.lastName(),
            request.birthYear()
        );

        entity.healthcareUnit = unit;
        entity.persist();

        return toResponse(entity);
    }

    @Transactional
    public PatientResponse updatePatient(
            Long id,
            PatientRequest request) {

        PatientEntity entity = PatientEntity.findById(id);

        if (entity == null) {
            throw new NotFoundException("Patient not found");
        }

        // Check access to the existing patient.
        requirePatientAccess(entity);

        // Also check access to the requested healthcare unit.
        requireTargetUnitAccess(request.healthcareUnitId());

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
        return findVisiblePatients(name, null)
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<PatientResponse> getPatientsByHealthcareUnit(Long unitId) {
        findHealthcareUnit(unitId);

        if (unitId == null) {
            throw new BadRequestException(
                "Healthcare unit ID is required"
            );
        }

        if (!accessService.isAdmin()) {
            accessService.requireUnitAccess(unitId);
        }

        return findVisiblePatients(null, unitId)
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException(
                "Page must be >= 0 and size must be between 1 and 100"
            );
        }
    }

    private PatientPageResponse toPageResponse(
            PanacheQuery<PatientEntity> query,
            int page,
            int size) {

        query.page(Page.of(page, size));

        long total = query.count();

        List<PatientResponse> patients = query.list()
            .stream()
            .map(this::toResponse)
            .toList();

        int totalPages = (int) ((total + size - 1) / size);

        return new PatientPageResponse(
            patients,
            page,
            size,
            total,
            totalPages
        );
    }

    public PatientPageResponse getPatientsPage(int page, int size) {
        validatePage(page, size);

        return toPageResponse(
            findVisiblePatients(null, null),
            page,
            size
        );
    }

    public PatientPageResponse searchPatientsPage(
            String name,
            int page,
            int size) {

        validatePage(page, size);

        return toPageResponse(
            findVisiblePatients(name, null),
            page,
            size
        );
    }

    public PatientPageResponse filterPatientsPage(
            String name,
            Long healthcareUnitId,
            int page,
            int size) {

        validatePage(page, size);

        return toPageResponse(
            findVisiblePatients(name, healthcareUnitId),
            page,
            size
        );
    }
}
