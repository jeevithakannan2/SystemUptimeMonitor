package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.model.Monitor;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MonitorDao {

    public void createMonitor(Connection connection, Monitor monitor) throws SQLException {
        String sql = "INSERT INTO monitors(name, target_url, check_interval, created_time, created_by, failure_count, organization, enabled, is_public) VALUES(?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, monitor.getName());
            pst.setString(2, monitor.getTargetUrl());
            pst.setInt(3, monitor.getCheckInterval());
            pst.setTimestamp(4, new Timestamp(monitor.getCreatedTime()));
            pst.setInt(5, monitor.getCreatedBy());
            pst.setInt(6, monitor.getFailureCount());
            pst.setString(7, monitor.getOrganization());
            pst.setBoolean(8, monitor.isEnabled());
            pst.setBoolean(9, monitor.isPublic());
            pst.executeUpdate();
            ResultSet generated = pst.getGeneratedKeys();
            if (generated.next())
                monitor.setId(generated.getInt(1));
        }
    }

    public Monitor getMonitorById(Connection connection, int monitorId) throws SQLException {
        String sql = "SELECT * FROM monitors WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Monitor monitor = new Monitor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5).getTime(), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getBoolean(9));
                monitor.setPublic(rs.getBoolean("is_public"));
                return monitor;
            }
        }
        return null;
    }

    public void deleteMonitor(Connection connection, int monitorId) throws SQLException {
        String sql = "DELETE FROM monitors WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            pst.executeUpdate();
        }
    }

    public Monitor getMonitorByURLAndOrg(Connection connection, String targetURL, String organization) throws SQLException {
        String sql = "SELECT * FROM monitors WHERE target_url=? AND organization=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, targetURL);
            pst.setString(2, organization);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Monitor monitor = new Monitor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5).getTime(), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getBoolean(9));
                monitor.setPublic(rs.getBoolean("is_public"));
                return monitor;
            }
        }
        return null;
    }

    public ArrayList<Monitor> getAllMonitorsByOrganization(Connection connection, String organization) throws SQLException {
        ArrayList<Monitor> monitors = new ArrayList<>();
        String sql = "SELECT * FROM monitors WHERE organization=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, organization);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Monitor monitor = new Monitor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5).getTime(), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getBoolean(9));
                monitor.setPublic(rs.getBoolean("is_public"));
                monitors.add(monitor);
            }
        }
        return monitors;
    }

    public ArrayList<Monitor> getAllMonitors(Connection connection) throws SQLException {
        ArrayList<Monitor> monitors = new ArrayList<>();
        String sql = "SELECT * FROM monitors";
        try (Statement st = connection.createStatement()) {
            ResultSet rs = st.executeQuery(sql);
            while (rs.next()) {
                Monitor monitor = new Monitor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5).getTime(), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getBoolean(9));
                monitor.setPublic(rs.getBoolean("is_public"));
                monitors.add(monitor);
            }
        }
        return monitors;
    }

    public void updateMonitor(Connection connection, Monitor monitor) throws SQLException {
        String sql = "UPDATE monitors SET name=?, target_url=?, check_interval=?, enabled=?, is_public=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, monitor.getName());
            pst.setString(2, monitor.getTargetUrl());
            pst.setInt(3, monitor.getCheckInterval());
            pst.setBoolean(4, monitor.isEnabled());
            pst.setBoolean(5, monitor.isPublic());
            pst.setInt(6, monitor.getId());
            pst.executeUpdate();
        }
    }

    public List<Monitor> getPublicMonitorsByOrganization(Connection connection, String organization) throws SQLException {
        List<Monitor> monitors = new ArrayList<>();
        String sql = "SELECT * FROM monitors WHERE organization=? AND is_public = true";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, organization);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Monitor monitor = new Monitor(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getInt(4), rs.getTimestamp(5).getTime(), rs.getInt(6), rs.getInt(7), rs.getString(8), rs.getBoolean(9));
                monitor.setPublic(rs.getBoolean("is_public"));
                monitors.add(monitor);
            }
        }
        return monitors;
    }
}
