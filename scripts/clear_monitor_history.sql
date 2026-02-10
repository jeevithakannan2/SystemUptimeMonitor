-- Clear monitor history (incidents, runs, audits) across all org schemas
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT s.schema_name FROM information_schema.schemata s WHERE s.schema_name LIKE 'org\_%'
    LOOP
        EXECUTE format('TRUNCATE TABLE %I.incidents, %I.monitor_runs, %I.monitor_audits CASCADE',
                       schema_name, schema_name, schema_name);
    END LOOP;
END $$;