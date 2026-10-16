
package se.carebridge.appointment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import se.carebridge.appointment.dto.AppointmentRequest;
import se.carebridge.appointment.dto.AppointmentResponse;
import se.carebridge.healthcare.HealthcareUnitEntity;
import se.carebridge.patient.PatientEntity;

import java.util.List;

@ApplicationScoped
public class AppointmentService {

  private AppointmentResponse toResponse(AppointmentEntity entity) {
    return new AppointmentResponse(
        entity.id,
        entity.patient.id,
        entity.healthcareUnit.id,
        entity.appointmentTime,
        entity.status);
  }

  public List<AppointmentResponse> getAllAppointments() {
    return AppointmentEntity.<AppointmentEntity>listAll()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  public AppointmentResponse getAppointmentById(Long id) {
    AppointmentEntity appointment = AppointmentEntity.findById(id);

    if (appointment == null) {
      throw new NotFoundException("Appointment not found");
    }

    return toResponse(appointment);
  }

  @Transactional
  public AppointmentResponse createAppointment(AppointmentRequest request) {

    PatientEntity patient = PatientEntity.findById(request.patientId());

    if (patient == null) {
      throw new NotFoundException("Patient not found");
    }

    HealthcareUnitEntity unit = HealthcareUnitEntity.findById(request.healthcareUnitId());

    if (unit == null) {
      throw new NotFoundException("Healthcare unit not found");
    }

    AppointmentEntity appointment = new AppointmentEntity();
    appointment.patient = patient;
    appointment.healthcareUnit = unit;
    appointment.appointmentTime = request.appointmentTime();
    appointment.status = AppointmentStatus.SCHEDULED;

    appointment.persistAndFlush();

    return toResponse(appointment);
  }

  @Transactional
  public AppointmentResponse updateAppointmentStatus(
      Long id, AppointmentStatus newStatus) {

    AppointmentEntity appointment = AppointmentEntity.findById(id);

    if (appointment == null) {
      throw new NotFoundException("Appointment not found");
    }

    if (newStatus == null) {
      throw new jakarta.ws.rs.BadRequestException(
          "Appointment status is required");
    }

    if (appointment.status != AppointmentStatus.SCHEDULED) {
      throw new jakarta.ws.rs.BadRequestException(
          "Only scheduled appointments can change status");
    }

    if (newStatus == AppointmentStatus.SCHEDULED) {
      throw new jakarta.ws.rs.BadRequestException(
          "Appointment is already scheduled");
    }

    appointment.status = newStatus;

    return toResponse(appointment);
  }

  public se.carebridge.appointment.dto.AppointmentPageResponse filterAppointmentsPage(
      String patientName,
      Long healthcareUnitId,
      AppointmentStatus status,
      int page,
      int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw new jakarta.ws.rs.BadRequestException(
          "Page must be >= 0 and size must be between 1 and 100");
    }

    if (healthcareUnitId != null &&
        HealthcareUnitEntity.findById(healthcareUnitId) == null) {
      throw new NotFoundException("Healthcare unit not found");
    }

    StringBuilder queryText = new StringBuilder("1 = 1");
    java.util.Map<String, Object> parameters = new java.util.HashMap<>();

    if (patientName != null && !patientName.isBlank()) {
      String search = "%" +
          patientName.trim().toLowerCase(java.util.Locale.ROOT) +
          "%";

      queryText.append(
          " AND (LOWER(patient.firstName) LIKE :patientName " +
              "OR LOWER(patient.lastName) LIKE :patientName)");

      parameters.put("patientName", search);
    }

    if (healthcareUnitId != null) {
      queryText.append(
          " AND healthcareUnit.id = :healthcareUnitId");

      parameters.put("healthcareUnitId", healthcareUnitId);
    }

    if (status != null) {
      queryText.append(" AND status = :status");
      parameters.put("status", status);
    }

    var query = AppointmentEntity.<AppointmentEntity>find(
        queryText.toString(),
        io.quarkus.panache.common.Sort.by("appointmentTime"),
        parameters).page(io.quarkus.panache.common.Page.of(page, size));

    long total = query.count();

    List<AppointmentResponse> appointments = query.list()
        .stream()
        .map(this::toResponse)
        .toList();

    int totalPages = (int) ((total + size - 1) / size);

    return new se.carebridge.appointment.dto.AppointmentPageResponse(
        appointments,
        page,
        size,
        total,
        totalPages);
  }

}
