package org.example.websitehealthmonitor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Logger;

public class DBManager {
    private static final Logger LOG = Logger.getLogger(DBManager.class.getName());
    private static final String url;
    private static final String username;
    private static final String password;

    static {
        Properties props = new Properties();
        // Look for .env in the working directory, then fall back to classpath
        File envFile = new File(".env");
        if (!envFile.exists()) {
            // Try common Tomcat-relative paths
            String catalinaBase = System.getProperty("catalina.base", "");
            envFile = new File(catalinaBase, ".env");
        }
        try {
            if (envFile.exists()) {
                try (FileInputStream fis = new FileInputStream(envFile)) {
                    props.load(fis);
                }
            } else {
                // Fall back to system environment variables
                if (System.getenv("DB_URL") != null) {
                    props.setProperty("DB_URL", System.getenv("DB_URL"));
                    props.setProperty("DB_USERNAME", System.getenv("DB_USERNAME"));
                    props.setProperty("DB_PASSWORD", System.getenv("DB_PASSWORD"));
                } else {
                    throw new RuntimeException(".env file not found and DB_URL environment variable not set");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load .env file", e);
        }

        url = props.getProperty("DB_URL");
        username = props.getProperty("DB_USERNAME");
        password = props.getProperty("DB_PASSWORD");

        LOG.info("Database configured: " + url);

        // Load PostgreSQL driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    /**
     * Returns a connection using the default public schema search path.
     * Use for the public organizations registry table.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Returns a connection with search_path set to the organization's schema + public.
     * All unqualified table references resolve first in the org schema, then in public.
     * Use for ALL org-scoped tables: users, invites, monitors, monitor_runs, incidents,
     * status_codes, monitor_audits.
     */
    public static Connection getConnection(String organization) throws SQLException {
        Connection conn = DriverManager.getConnection(url, username, password);
        String schema = SchemaManager.toSchemaName(organization);
        String quotedSchema = "\"" + schema.replace("\"", "\"\"") + "\"";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + quotedSchema + ", public");
        }
        return conn;
    }

    /**
     * Initializes the public-schema organizations registry table and ensures
     * org schemas exist for every registered organization.
     */
    public static void initSchema() throws SQLException {
        // Public schema: only the organizations registry
        String ddl = "CREATE TABLE IF NOT EXISTS public.organizations ("
                + "  id SERIAL PRIMARY KEY,"
                + "  name VARCHAR(255) NOT NULL UNIQUE,"
                + "  schema_name VARCHAR(255) NOT NULL UNIQUE"
                + ");";

        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }

        // Ensure all registered orgs have their schemas initialized
        initExistingOrgSchemas();
    }

    /**
     * Ensures every organization registered in public.organizations has its schema
     * and tables created.
     */
    private static void initExistingOrgSchemas() {
        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM public.organizations")) {
            while (rs.next()) {
                String org = rs.getString("name");
                SchemaManager.createOrgSchema(connection, org);
            }
        } catch (SQLException e) {
            LOG.fine("Could not initialize existing org schemas: " + e.getMessage());
        }
    }
}
