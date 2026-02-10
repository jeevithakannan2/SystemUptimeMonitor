package org.example.systemuptimemonitor.util;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DBManager {
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

        // Load PostgreSQL driver
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void initSchema() throws SQLException {
        String ddl = ""
            + "CREATE TABLE IF NOT EXISTS users ("
            + "  id SERIAL PRIMARY KEY,"
            + "  email VARCHAR(255) NOT NULL UNIQUE,"
            + "  password VARCHAR(255) NOT NULL,"
            + "  role VARCHAR(50) NOT NULL,"
            + "  organization VARCHAR(255) NOT NULL"
            + ");"
            + "CREATE TABLE IF NOT EXISTS monitors ("
            + "  id SERIAL PRIMARY KEY,"
            + "  name VARCHAR(255) NOT NULL,"
            + "  target_url VARCHAR(2048) NOT NULL,"
            + "  check_interval INTEGER NOT NULL,"
            + "  created_time TIMESTAMP NOT NULL,"
            + "  created_by INTEGER NOT NULL REFERENCES users(id),"
            + "  failure_count INTEGER NOT NULL DEFAULT 0,"
            + "  organization VARCHAR(255) NOT NULL,"
            + "  enabled BOOLEAN NOT NULL DEFAULT TRUE"
            + ");"
            + "CREATE TABLE IF NOT EXISTS monitor_runs ("
            + "  id SERIAL PRIMARY KEY,"
            + "  monitor_id INTEGER NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,"
            + "  time TIMESTAMP NOT NULL,"
            + "  response_time INTEGER NOT NULL,"
            + "  status_code INTEGER NOT NULL,"
            + "  success BOOLEAN NOT NULL"
            + ");"
            + "CREATE TABLE IF NOT EXISTS incidents ("
            + "  id SERIAL PRIMARY KEY,"
            + "  monitor_run_id INTEGER NOT NULL REFERENCES monitor_runs(id) ON DELETE CASCADE,"
            + "  down_time TIMESTAMP NOT NULL,"
            + "  resolved_time TIMESTAMP,"
            + "  status_code INTEGER NOT NULL,"
            + "  resolved BOOLEAN NOT NULL DEFAULT FALSE,"
            + "  notes TEXT"
            + ");"
            + "CREATE TABLE IF NOT EXISTS status_codes ("
            + "  monitor_id INTEGER NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,"
            + "  status_code INTEGER NOT NULL,"
            + "  PRIMARY KEY (monitor_id, status_code)"
            + ");"
            + "CREATE TABLE IF NOT EXISTS invites ("
            + "  id SERIAL PRIMARY KEY,"
            + "  created_by INTEGER NOT NULL REFERENCES users(id),"
            + "  created_time TIMESTAMP NOT NULL,"
            + "  expired BOOLEAN NOT NULL DEFAULT FALSE,"
            + "  url VARCHAR(255) NOT NULL UNIQUE,"
            + "  role VARCHAR(50) NOT NULL"
            + ");"
            + "CREATE TABLE IF NOT EXISTS monitor_audits ("
            + "  id SERIAL PRIMARY KEY,"
            + "  monitor_id INTEGER NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,"
            + "  operation VARCHAR(50) NOT NULL,"
            + "  time TIMESTAMP NOT NULL"
            + ");";

        try (Connection connection = getConnection();
             Statement stmt = connection.createStatement()) {
            stmt.execute(ddl);
        }
    }
}
