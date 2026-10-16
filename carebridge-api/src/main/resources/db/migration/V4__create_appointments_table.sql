
CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,

    patient_id BIGINT NOT NULL,

    healthcare_unit_id BIGINT NOT NULL,

    appointment_time TIMESTAMP NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',

    CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id)
        REFERENCES patients(id),

    CONSTRAINT fk_appointments_healthcare_unit
        FOREIGN KEY (healthcare_unit_id)
        REFERENCES healthcare_units(id),

    CONSTRAINT chk_appointments_status
        CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX idx_appointments_patient
    ON appointments(patient_id);

CREATE INDEX idx_appointments_healthcare_unit
    ON appointments(healthcare_unit_id);

CREATE INDEX idx_appointments_time
    ON appointments(appointment_time);
