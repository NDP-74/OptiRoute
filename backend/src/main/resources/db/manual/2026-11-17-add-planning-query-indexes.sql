BEGIN;

CREATE INDEX IF NOT EXISTS idx_route_start_date
    ON route (start_date);

CREATE INDEX IF NOT EXISTS idx_route_driver_start_date
    ON route (driver_id, start_date);

CREATE INDEX IF NOT EXISTS idx_route_service_id_id
    ON route (service_id, id);

CREATE INDEX IF NOT EXISTS idx_event_route_id
    ON event (route_id);

CREATE INDEX IF NOT EXISTS idx_cost_condition_parameter_id
    ON cost_condition (cost_parameter_id);

COMMIT;
