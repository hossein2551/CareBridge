
package se.carebridge.appointment;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import se.carebridge.appointment.dto.AppointmentPageResponse;
import se.carebridge.appointment.dto.AppointmentRequest;
import se.carebridge.appointment.dto.AppointmentResponse;
import se.carebridge.healthcare.HealthcareUnitEntity;
import se.carebridge.patient.PatientEntity;
import se.carebridge.security.HealthcareAccessService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class AppointmentService {

    private static final String DOUBLE_BOOKING_MESSAGE =
        "This healthcare unit already has an appointment at the selected time";

    private static final String DOUBLE_BOOKING_INDEX =
        "uq_appointments_scheduled_unit_time";

    @Inject
    HealthcareAccessService accessService;

    private AppointmentResponse toResponse(AppointmentEntity entity) {
        return new AppointmentResponse(
            entity.id,
            entity.patient.id,
            entity.healthcareUnit.id,
            entity.appointmentTime,
            entity.status
        );
    }

    private void requirePatientAccess(PatientEntity patient) {
        Long unitId = patient.healthcareUnit == null
            ? null
            : patient.healthcareUnit.id;

        if (accessService.isAdmin()) {
            return;
        }

        accessService.requireUnitAccess(unitId);
    }

    private void requireAppointmentAccess(AppointmentEntity appointment) {
        if (accessService.isAdmin()) {
            return;
        }

        accessService.requireUnitAccess(appointment.healthcareUnit.id);
        requirePatientAccess(appointment.patient);
    }

    private PanacheQuery<AppointmentEntity> findVisibleAppointments(
            String patientName,
            Long healthcareUnitId,
            AppointmentStatus status) {

        StringBuilder queryText = new StringBuilder("1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (patientName != null && !patientName.isBlank()) {
            String search = "%" +
                patientName.trim().toLowerCase(Locale.ROOT) + "%";

            queryText.append(
                " AND (LOWER(patient.firstName) LIKE :patientName " +
                "OR LOWER(patient.lastName) LIKE :patientName)"
            );

            parameters.put("patientName", search);
        }

        if (healthcareUnitId != null) {
            if (HealthcareUnitEntity.findById(healthcareUnitId) == null) {
                throw new NotFoundException("Healthcare unit not found");
            }

            accessService.requireUnitAccess(healthcareUnitId);

            queryText.append(
                " AND healthcareUnit.id = :healthcareUnitId"
            );

            parameters.put("healthcareUnitId", healthcareUnitId);
        }

        if (status != null) {
            queryText.append(" AND status = :status");
            parameters.put("status", status);
        }

        if (!accessService.isAdmin()) {
            List<Long> allowedIds = accessService.getAllowedUnitIds();

            if (allowedIds.isEmpty()) {
                queryText.append(" AND 1 = 0");
            } else {
                queryText.append(
                    " AND healthcareUnit.id IN :allowedUnitIds" +
                    " AND patient.healthcareUnit.id IN :allowedUnitIds"
                );

                parameters.put("allowedUnitIds", allowedIds);
            }
        }

        return AppointmentEntity.<AppointmentEntity>find(
            queryText.toString(),
            Sort.by("appointmentTime"),
            parameters
        );
    }

    private WebApplicationException doubleBookingConflict() {
        return new WebApplicationException(
            Response.status(Response.Status.CONFLICT)
                .type(MediaType.TEXT_PLAIN)
                .entity(DOUBLE_BOOKING_MESSAGE)
                .build()
        );
    }

    private boolean isDoubleBookingConstraintViolation(Throwable error) {
        Throwable current = error;

        while (current != null) {
            if (current instanceof SQLException sqlException) {
                SQLException next = sqlException;

                while (next != null) {
                    if ("23505".equals(next.getSQLState())
                            && next.getMessage() != null
                            && next.getMessage().contains(DOUBLE_BOOKING_INDEX)) {
                        return true;
                    }

                    next = next.getNextException();
                }
            }

            current = current.getCause();
        }

        return false;
    }

    public List<AppointmentResponse> getAllAppointments() {
        return findVisibleAppointments(null, null, null)
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public List<AppointmentResponse> getAppointmentsByPatient(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new BadRequestException("A valid patient ID is required");
        }

        PatientEntity patient = PatientEntity.findById(patientId);

        if (patient == null) {
            throw new NotFoundException("Patient not found");
        }

        requirePatientAccess(patient);

        if (accessService.isAdmin()) {
            return AppointmentEntity.<AppointmentEntity>find(
                "patient.id = ?1 ORDER BY appointmentTime DESC",
                patientId
            )
            .list()
            .stream()
            .map(this::toResponse)
            .toList();
        }

        List<Long> allowedIds = accessService.getAllowedUnitIds();

        return AppointmentEntity.<AppointmentEntity>find(
            "patient.id = :patientId " +
            "AND healthcareUnit.id IN :allowedIds " +
            "ORDER BY appointmentTime DESC",
            Map.of(
                "patientId", patientId,
                "allowedIds", allowedIds
            )
        )
        .list()
        .stream()
        .map(this::toResponse)
        .toList();
    }

    public AppointmentResponse getAppointmentById(Long id) {
        AppointmentEntity appointment = AppointmentEntity.findById(id);

        if (appointment == null) {
            throw new NotFoundException("Appointment not found");
        }

        requireAppointmentAccess(appointment);

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        PatientEntity patient = PatientEntity.findById(request.patientId());

        if (patient == null) {
            throw new NotFoundException("Patient not found");
        }

        HealthcareUnitEntity unit =
            HealthcareUnitEntity.findById(request.healthcareUnitId());

        if (unit == null) {
            throw new NotFoundException("Healthcare unit not found");
        }

        requirePatientAccess(patient);
        accessService.requireUnitAccess(unit.id);

        if (!accessService.isAdmin()
                && !unit.id.equals(patient.healthcareUnit.id)) {
            throw new BadRequestException(
                "Patient must belong to the selected healthcare unit"
            );
        }

        long existingBookings = AppointmentEntity.count(
            "healthcareUnit.id = ?1 AND appointmentTime = ?2 AND status = ?3",
            request.healthcareUnitId(),
            request.appointmentTime(),
            AppointmentStatus.SCHEDULED
        );

        if (existingBookings > 0) {
            throw doubleBookingConflict();
        }

        AppointmentEntity appointment = new AppointmentEntity();
        appointment.patient = patient;
        appointment.healthcareUnit = unit;
        appointment.appointmentTime = request.appointmentTime();
        appointment.status = AppointmentStatus.SCHEDULED;

        try {
            appointment.persistAndFlush();
        } catch (RuntimeException error) {
            if (isDoubleBookingConstraintViolation(error)) {
                throw doubleBookingConflict();
            }

            throw error;
        }

        return toResponse(appointment);
    }

    @Transactional
    public AppointmentResponse updateAppointmentStatus(
            Long id,
            AppointmentStatus newStatus) {

        AppointmentEntity appointment = AppointmentEntity.findById(id);

        if (appointment == null) {
            throw new NotFoundException("Appointment not found");
        }

        requireAppointmentAccess(appointment);

        if (newStatus == null) {
            throw new BadRequestException(
                "Appointment status is required"
            );
        }

        if (appointment.status != AppointmentStatus.SCHEDULED) {
            throw new BadRequestException(
                "Only scheduled appointments can change status"
            );
        }

        if (newStatus == AppointmentStatus.SCHEDULED) {
            throw new BadRequestException(
                "Appointment is already scheduled"
            );
        }

        appointment.status = newStatus;

        return toResponse(appointment);
    }

    public AppointmentPageResponse filterAppointmentsPage(
            String patientName,
            Long healthcareUnitId,
            AppointmentStatus status,
            int page,
            int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException(
                "Page must be >= 0 and size must be between 1 and 100"
            );
        }

        var query = findVisibleAppointments(
            patientName,
            healthcareUnitId,
            status
        ).page(Page.of(page, size));

        long total = query.count();

        List<AppointmentResponse> appointments = query.list()
            .stream()
            .map(this::toResponse)
            .toList();

        int totalPages = (int) ((total + size - 1) / size);

        return new AppointmentPageResponse(
            appointments,
            page,
            size,
            total,
            totalPages
        );
    }

    public List<String> getBookedTimes(
            Long healthcareUnitId,
            LocalDate date) {

        if (healthcareUnitId == null || date == null) {
            throw new BadRequestException(
                "healthcareUnitId and date are required"
            );
        }

        if (HealthcareUnitEntity.findById(healthcareUnitId) == null) {
            throw new NotFoundException("Healthcare unit not found");
        }

        accessService.requireUnitAccess(healthcareUnitId);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime startOfNextDay = date.plusDays(1).atStartOfDay();

        List<AppointmentEntity> bookedAppointments =
            AppointmentEntity.<AppointmentEntity>list(
                "healthcareUnit.id = ?1 " +
                "AND appointmentTime >= ?2 " +
                "AND appointmentTime < ?3 " +
                "AND status = ?4 " +
                "ORDER BY appointmentTime",
                healthcareUnitId,
                startOfDay,
                startOfNextDay,
                AppointmentStatus.SCHEDULED
            );

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        return bookedAppointments.stream()
            .map(appointment -> appointment.appointmentTime.format(formatter))
            .distinct()
            .toList();
    }
}
