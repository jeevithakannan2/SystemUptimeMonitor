-- Migration: Add email notification support
-- Run this against existing databases to add notification features.
-- Safe to run multiple times (uses IF NOT EXISTS / ADD COLUMN IF NOT EXISTS).

-- For each org schema, add email_notifications column and monitor_subscriptions table.
-- Replace 'org_example_com' with your actual schema name, or run for each schema.

DO $$
DECLARE
    schema_rec RECORD;
BEGIN
    FOR schema_rec IN SELECT schema_name FROM public.organizations LOOP
        EXECUTE format('ALTER TABLE %I.users ADD COLUMN IF NOT EXISTS email_notifications BOOLEAN NOT NULL DEFAULT FALSE', schema_rec.schema_name);
        EXECUTE format('CREATE TABLE IF NOT EXISTS %I.monitor_subscriptions (
            user_id INTEGER NOT NULL REFERENCES %I.users(id) ON DELETE CASCADE,
            monitor_id INTEGER NOT NULL REFERENCES %I.monitors(id) ON DELETE CASCADE,
            PRIMARY KEY (user_id, monitor_id)
        )', schema_rec.schema_name, schema_rec.schema_name, schema_rec.schema_name);
    END LOOP;
END $$;
