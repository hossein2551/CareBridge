
package se.carebridge.security;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "staff_healthcare_units")
public class StaffHealthcareUnitEntity extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "keycloak_user_id", nullable = false, length = 100)
  public String keycloakUserId;

  @Column(name = "healthcare_unit_id", nullable = false)
  public Long healthcareUnitId;
}
