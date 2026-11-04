BEGIN;

ALTER TABLE cost_parameter
    DROP CONSTRAINT IF EXISTS ck_cost_parameter_assignment_type;

ALTER TABLE cost_parameter
    ALTER COLUMN assignment_type SET DEFAULT 'ALWAYS';

ALTER TABLE cost_parameter
    ADD CONSTRAINT ck_cost_parameter_assignment_type
    CHECK (assignment_type IN (
        'ALWAYS', 'ALL_CONDITIONS', 'ANY_CONDITION', 'MANUAL'
    ));

COMMIT;
