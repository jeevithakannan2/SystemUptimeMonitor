package org.example.systemuptimemonitor.dao;

import java.sql.*;

public class MonitorAuditDao {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";

    public void createAudit(Connection connection, int monitorId, String operation) throws SQLException {
        String sql = "INSERT INTO monitor_audits (monitor_id, operation, time) VALUES (?,?,?)";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            pst.setString(2, operation);
            pst.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            pst.executeUpdate();
        }
    }

    public boolean inAudit(int monitorId) throws SQLException {
        String sql = "SELECT * FROM monitor_audits WHERE id=? AND operation != CREATED";
        try(Connection connection = DriverManager.getConnection(url , username, password)) {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }
}
