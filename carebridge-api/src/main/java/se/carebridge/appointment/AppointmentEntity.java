
package se.carebridge.appointment;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import se.carebridge.patient.PatientEntity;
import se.carebridge.healthcare.HealthcareUnitEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
public class AppointmentEntity extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "patient_id", nullable = false)
  public PatientEntity patient;

  @ManyToOne(optional = false)
  @JoinColumn(name = "healthcare_unit_id", nullable = false)
  public HealthcareUnitEntity healthcareUnit;

  @Column(name = "appointment_time", nullable = false)
  public LocalDateTime appointmentTime;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AppointmentStatus status;
}
