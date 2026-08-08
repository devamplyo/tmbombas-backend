ALTER TABLE maintenance_plans ADD COLUMN released      BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE maintenance_plans ADD COLUMN technician_id BIGINT;

ALTER TABLE maintenance_plans
    ADD CONSTRAINT fk_maintenance_plans_technician
    FOREIGN KEY (technician_id) REFERENCES usuarios (id) ON DELETE SET NULL;

CREATE INDEX idx_maintenance_plans_technician ON maintenance_plans (technician_id);
CREATE INDEX idx_maintenance_plans_released   ON maintenance_plans (released);