package se.carebridge.healthcare;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.NotFoundException;

import se.carebridge.healthcare.dto.HealthcareUnitRequest;
import se.carebridge.healthcare.dto.HealthcareUnitResponse;

import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class HealthcareUnitService {

    private HealthcareUnitResponse toResponse(HealthcareUnitEntity entity) {
        return new HealthcareUnitResponse(
            entity.id,
            entity.name,
            entity.city,
            entity.organizationCode,
            entity.createdAt
        );
    }

    public List<HealthcareUnitResponse> getAllUnits() {
        return HealthcareUnitEntity.<HealthcareUnitEntity>listAll()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public HealthcareUnitResponse getUnitById(Long id) {
        HealthcareUnitEntity entity = HealthcareUnitEntity.findById(id);

        if (entity == null) {
            throw new NotFoundException("Healthcare unit not found");
        }

        return toResponse(entity);
    }

    public List<HealthcareUnitResponse> searchByCity(String city) {
        String search = "%" + city.trim().toLowerCase(Locale.ROOT) + "%";

        return HealthcareUnitEntity.<HealthcareUnitEntity>list(
            "LOWER(city) LIKE ?1", search
        )
        .stream()
        .map(this::toResponse)
        .toList();
    }

    @Transactional
    public HealthcareUnitResponse createUnit(HealthcareUnitRequest request) {

        String code = request.organizationCode().trim();

        long existing = HealthcareUnitEntity.count(
            "organizationCode", code
        );

        if (existing > 0) {
            throw new WebApplicationException(
                "Organization code already exists", 409
            );
        }

        HealthcareUnitEntity entity = new HealthcareUnitEntity(
            request.name().trim(),
            request.city().trim(),
            code
        );

        entity.persistAndFlush();
        HealthcareUnitEntity.getEntityManager().refresh(entity);

        return toResponse(entity);
    }
}