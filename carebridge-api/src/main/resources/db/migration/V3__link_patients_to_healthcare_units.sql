ALTER TABLE patients
ADD COLUMN healthcare_unit_id BIGINT;

ALTER TABLE patients
ADD CONSTRAINT fk_patients_healthcare_unit
FOREIGN KEY (healthcare_unit_id)
REFERENCES healthcare_units(id)
ON DELETE RESTRICT;

CREATE INDEX idx_patients_healthcare_unit
ON patients(healthcare_unit_id);