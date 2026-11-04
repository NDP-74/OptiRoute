BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM cost_condition cc
        LEFT JOIN cost_rule cr ON cr.id = cc.cost_rule_id
        LEFT JOIN cost_parameter cp ON cp.id = cr.cost_parameter_id
        WHERE cp.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot migrate orphan cost conditions: restore their cost parameter or explicitly remove them before migration';
    END IF;

    IF EXISTS (
        SELECT cr.cost_parameter_id
        FROM cost_rule cr
        GROUP BY cr.cost_parameter_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Cannot migrate cost parameters with multiple cost rules';
    END IF;
END;
$$;

ALTER TABLE cost_condition ADD COLUMN cost_parameter_id BIGINT;

UPDATE cost_condition cc
SET cost_parameter_id = cr.cost_parameter_id
FROM cost_rule cr
WHERE cc.cost_rule_id = cr.id;

ALTER TABLE cost_parameter
    DROP CONSTRAINT IF EXISTS ck_cost_parameter_assignment_type;

UPDATE cost_parameter cp
SET assignment_type = CASE
    WHEN cp.assignment_type = 'MANUAL' THEN 'MANUAL'
    WHEN EXISTS (
        SELECT 1
        FROM cost_rule cr
        WHERE cr.cost_parameter_id = cp.id
          AND cr.logical_operator = 'OR'
    ) THEN 'ANY_CONDITION'
    ELSE 'ALL_CONDITIONS'
END;

DELETE FROM cost_condition cc
USING cost_parameter cp
WHERE cc.cost_parameter_id = cp.id
  AND cp.assignment_type = 'MANUAL';

ALTER TABLE cost_condition ALTER COLUMN cost_parameter_id SET NOT NULL;
ALTER TABLE cost_condition DROP COLUMN cost_rule_id;
ALTER TABLE cost_condition
    ADD CONSTRAINT fk_cost_condition_parameter
    FOREIGN KEY (cost_parameter_id) REFERENCES cost_parameter(id);
DROP TABLE cost_rule;

ALTER TABLE cost_parameter
    ALTER COLUMN assignment_type SET DEFAULT 'ALWAYS';

ALTER TABLE cost_parameter
    ADD CONSTRAINT ck_cost_parameter_assignment_type
    CHECK (assignment_type IN (
        'ALWAYS', 'ALL_CONDITIONS', 'ANY_CONDITION', 'MANUAL'
    ));

COMMIT;
