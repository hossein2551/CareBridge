package se.carebridge.patient;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "patients")
public class PatientEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    @Column(name = "first_name", nullable = false)
    public String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    @Column(name = "last_name", nullable = false)
    public String lastName;

    @NotNull(message = "Birth year is required")
    @Min(value = 1900, message = "Birth year must be 1900 or later")
    @Max(value = 2100, message = "Birth year must be 2100 or earlier")
    @Column(name = "birth_year", nullable = false)
    public Integer birthYear;

    public PatientEntity() {
    }

    public PatientEntity(String firstName, String lastName, Integer birthYear) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthYear = birthYear;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "healthcare_unit_id")
    public se.carebridge.healthcare.HealthcareUnitEntity healthcareUnit;
}