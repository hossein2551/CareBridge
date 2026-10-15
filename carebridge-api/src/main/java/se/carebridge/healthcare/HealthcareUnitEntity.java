package se.carebridge.healthcare;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "healthcare_units")
public class HealthcareUnitEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Size(max = 150)
    @Column(name = "name", nullable = false, length = 150)
    public String name;

    @NotBlank
    @Size(max = 100)
    @Column(name = "city", nullable = false, length = 100)
    public String city;

    @NotBlank
    @Size(max = 50)
    @Column(name = "organization_code", nullable = false, unique = true, length = 50)
    public String organizationCode;

    @Column(name = "created_at", insertable = false, updatable = false)
    public java.time.OffsetDateTime createdAt;

    public HealthcareUnitEntity() {
    }

    public HealthcareUnitEntity(String name, String city, String organizationCode) {
        this.name = name;
        this.city = city;
        this.organizationCode = organizationCode;
    }
}