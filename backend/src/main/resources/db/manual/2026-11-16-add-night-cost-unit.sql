BEGIN;

ALTER TABLE cost_parameter DROP CONSTRAINT IF EXISTS cost_parameter_unit_check;
ALTER TABLE cost_parameter DROP CONSTRAINT IF EXISTS ck_cost_parameter_unit;
ALTER TABLE cost_parameter ADD CONSTRAINT ck_cost_parameter_unit CHECK (unit IN (
    'EUR_PER_KM', 'EUR_PER_TRIP', 'EUR_PER_HOUR', 'EUR_PER_DAY', 'EUR_PER_NIGHT', 'EUR_PER_MONTH', 'EUR_PER_YEAR'
));

COMMIT;
