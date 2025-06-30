package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.model.Monitor;

import java.sql.*;

public class MonitorDao {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";

    public void createMonitor(Connection connection, Monitor monitor) throws SQLException {
        String sql = "INSERT INTO monitors(name, target_url, check_interval, created_time, created_by, organization, enabled) VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, monitor.getName());
            pst.setString(2, monitor.getTargetUrl());
            pst.setInt(3, monitor.getCheckInterval());
            pst.setTimestamp(4, new Timestamp(monitor.getCreatedTime()));
            pst.setInt(5, monitor.getCreatedBy());
            pst.setString(6, monitor.getOrganization());
            pst.setBoolean(7, monitor.isEnabled());
            pst.executeUpdate();
        }
    }
}
