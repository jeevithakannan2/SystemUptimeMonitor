package org.example.systemuptimemonitor.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages per-organization PostgreSQL schemas for tenant isolation.
 * <p>
 * The public schema holds only an {@code organizations} registry table.
 * Each organization gets its own schema (e.g., "org_example_com") containing
 * ALL tables: users, invites, monitors, monitor_runs, incidents, status_codes,
 * and monitor_audits.
 */
public class SchemaManager {
    private static final Logger LOG = Logger.getLogger(SchemaManager.class.getName());

    /**
     * Converts an organization name (e.g., "example.com") to a valid PostgreSQL schema name.
     * Replaces non-alphanumeric characters with underscores and prefixes with "org_".
     */
    public static String toSchemaName(String organization) {
        if (organization == null || organization.isEmpty()) {
            throw new IllegalArgumentException("Organization name cannot be null or empty");
        }
        String sanitized = organization.toLowerCase().replaceAll("[^a-z0-9]", "_");
        return "org_" + sanitized;
    }

    /**
     * Registers a new organization in the public.organizations table and
     * creates its schema with all required tables.
     * Safe to call multiple times — uses IF NOT EXISTS / ON CONFLICT.
     */
    public static void createOrgSchema(Connection connection, String organization) throws SQLException {
        String schema = toSchemaName(organization);
        LOG.info("Creating schema for organization: " + organization + " -> " + schema);

        // Register in the public organizations table
        String insertSql = "INSERT INTO public.organizations (name, schema_name) VALUES (?, ?) ON CONFLICT (name) DO NOTHING";
        try (PreparedStatement pst = connection.prepareStatement(insertSql)) {
            pst.setString(1, organization);
            pst.setString(2, schema);
            pst.executeUpdate();
        }

        // Create the schema
        String quotedSchema = "\"" + schema.replace("\"", "\"\"") + "\"";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + quotedSchema);
        }

        initOrgTables(connection, schema);
    }

    /**
     * Creates all org-scoped tables within the given schema.
     * All tables are self-contained within the schema — no cross-schema FKs.
     */
    private static void initOrgTables(Connection connection, String schema) throws SQLException {
        String q = "\"" + schema.replace("\"", "\"\"") + "\"";

        String[] ddls = {
            // Users table — org-scoped
            "CREATE TABLE IF NOT EXISTS " + q + ".users ("
                + "  id SERIAL PRIMARY KEY,"
                + "  email VARCHAR(255) NOT NULL UNIQUE,"
                + "  password VARCHAR(255) NOT NULL,"
                + "  role VARCHAR(50) NOT NULL,"
                + "  organization VARCHAR(255) NOT NULL,"
                + "  email_notifications BOOLEAN NOT NULL DEFAULT FALSE"
                + ")",

            // Invites table — org-scoped, references org users
            "CREATE TABLE IF NOT EXISTS " + q + ".invites ("
                + "  id SERIAL PRIMARY KEY,"
                + "  created_by INTEGER NOT NULL REFERENCES " + q + ".users(id),"
                + "  created_time TIMESTAMP NOT NULL,"
                + "  expired BOOLEAN NOT NULL DEFAULT FALSE,"
                + "  url VARCHAR(255) NOT NULL UNIQUE,"
                + "  role VARCHAR(50) NOT NULL"
                + ")",

            // Monitors
            "CREATE TABLE IF NOT EXISTS " + q + ".monitors ("
                + "  id SERIAL PRIMARY KEY,"
                + "  name VARCHAR(255) NOT NULL,"
                + "  target_url VARCHAR(2048) NOT NULL,"
                + "  check_interval INTEGER NOT NULL,"
                + "  created_time TIMESTAMP NOT NULL,"
                + "  created_by INTEGER NOT NULL REFERENCES " + q + ".users(id),"
                + "  failure_count INTEGER NOT NULL DEFAULT 0,"
                + "  organization VARCHAR(255) NOT NULL,"
                + "  enabled BOOLEAN NOT NULL DEFAULT TRUE,"
                + "  is_public BOOLEAN NOT NULL DEFAULT FALSE"
                + ")",

            // Monitor runs
            "CREATE TABLE IF NOT EXISTS " + q + ".monitor_runs ("
                + "  id SERIAL PRIMARY KEY,"
                + "  monitor_id INTEGER NOT NULL REFERENCES " + q + ".monitors(id) ON DELETE CASCADE,"
                + "  time TIMESTAMP NOT NULL,"
                + "  response_time INTEGER NOT NULL,"
                + "  status_code INTEGER NOT NULL,"
                + "  success BOOLEAN NOT NULL"
                + ")",

            // Incidents
            "CREATE TABLE IF NOT EXISTS " + q + ".incidents ("
                + "  id SERIAL PRIMARY KEY,"
                + "  monitor_run_id INTEGER NOT NULL REFERENCES " + q + ".monitor_runs(id) ON DELETE CASCADE,"
                + "  down_time TIMESTAMP NOT NULL,"
                + "  resolved_time TIMESTAMP,"
                + "  status_code INTEGER NOT NULL,"
                + "  expected_status_codes VARCHAR(255),"
                + "  resolved BOOLEAN NOT NULL DEFAULT FALSE,"
                + "  notes TEXT"
                + ")",

            // Status codes
            "CREATE TABLE IF NOT EXISTS " + q + ".status_codes ("
                + "  monitor_id INTEGER NOT NULL REFERENCES " + q + ".monitors(id) ON DELETE CASCADE,"
                + "  status_code INTEGER NOT NULL,"
                + "  PRIMARY KEY (monitor_id, status_code)"
                + ")",

            // Monitor audits (no FK to monitors — audits must survive monitor deletion)
            "CREATE TABLE IF NOT EXISTS " + q + ".monitor_audits ("
                + "  id SERIAL PRIMARY KEY,"
                + "  monitor_id INTEGER NOT NULL,"
                + "  operation VARCHAR(50) NOT NULL,"
                + "  time TIMESTAMP NOT NULL"
                + ")",

            // Monitor notification subscriptions
            "CREATE TABLE IF NOT EXISTS " + q + ".monitor_subscriptions ("
                + "  user_id INTEGER NOT NULL REFERENCES " + q + ".users(id) ON DELETE CASCADE,"
                + "  monitor_id INTEGER NOT NULL REFERENCES " + q + ".monitors(id) ON DELETE CASCADE,"
                + "  PRIMARY KEY (user_id, monitor_id)"
                + ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String ddl : ddls) {
                stmt.execute(ddl);
            }
        }
        LOG.info("Org tables initialized for schema: " + schema);
    }

    /**
     * Checks whether an organization exists in the public.organizations registry.
     */
    public static boolean organizationExists(String organization) throws SQLException {
        String sql = "SELECT 1 FROM public.organizations WHERE name = ?";
        try (Connection connection = DBManager.getConnection();
             PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, organization);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Returns all registered organization names from the public.organizations table.
     */
    public static List<String> getAllOrganizations() throws SQLException {
        List<String> orgs = new ArrayList<>();
        String sql = "SELECT name FROM public.organizations";
        try (Connection connection = DBManager.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                orgs.add(rs.getString("name"));
            }
        }
        return orgs;
    }
}
