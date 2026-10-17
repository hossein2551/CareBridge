
-- Prevent two scheduled appointments at the same healthcare unit
-- from having the same start time.
-- Completed and cancelled appointments do not block the time slot.

CREATE UNIQUE INDEX uq_appointments_scheduled_unit_time
ON appointments (healthcare_unit_id, appointment_time)
WHERE status = 'SCHEDULED';
