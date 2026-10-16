
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

}
