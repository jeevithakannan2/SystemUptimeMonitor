-- Clear ALL data across all org schemas and the public organizations registry
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT s.schema_name FROM information_schema.schemata s WHERE s.schema_name LIKE 'org\_%'
    LOOP
        EXECUTE format('DROP SCHEMA %I CASCADE', schema_name);
    END LOOP;
END $$;

TRUNCATE TABLE organizations CASCADE;
