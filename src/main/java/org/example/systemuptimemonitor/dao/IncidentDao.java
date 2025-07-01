package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.model.Incident;

import java.sql.*;
import java.util.ArrayList;

public class IncidentDao {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";

    public void createIncident(Connection connection, Incident incident) throws SQLException {
        String sql = "INSERT INTO incidents (monitor_run_id, down_time, status_code) VALUES (?,?,?)";
        try(PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);) {
            pst.setInt(1, incident.getMonitorRunId());
            pst.setTimestamp(2, new Timestamp(incident.getDownTime()));
            pst.setInt(3, incident.getStatusCode());
            pst.executeUpdate();
            ResultSet generatedId = pst.getGeneratedKeys();
            if(generatedId.next()) incident.setId(generatedId.getInt(1));
        }
    }

    public ArrayList<Incident> getIncidentsByMonitor(Connection connection, int monitorId) throws SQLException {
        ArrayList<Incident> incidents = new ArrayList<>();
        String sql = "SELECT i.* FROM incidents i JOIN monitor_runs mr ON mr.id=i.monitor_run_id JOIN monitors m ON m.id=mr.monitor_id WHERE m.id=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("resolved_time");
                long time = 0;
                if (ts != null)
                    time = ts.getTime();
                incidents.add(new Incident(rs.getInt("id"), rs.getInt("monitor_run_id"), rs.getTimestamp("down_time").getTime(), time, rs.getInt("status_code"), rs.getBoolean("resolved")));

            }
        }
        return incidents;
    }

    public ArrayList<Incident> getIncidentsByOrganization(Connection connection, String organization) throws SQLException {
        ArrayList<Incident> incidents = new ArrayList<>();
        String sql = "SELECT i.* FROM incidents i JOIN monitor_runs mr ON mr.id=i.monitor_run_id JOIN monitors m ON m.id=mr.monitor_id WHERE organization=?";
        try (PreparedStatement pst = connection.prepareStatement(sql);) {
            pst.setString(1, organization);
            ResultSet rs = pst.executeQuery();
            while(rs.next()) {
                Timestamp ts = rs.getTimestamp("resolved_time");
                long time = 0;
                if (ts != null)
                    time = ts.getTime();
                incidents.add(new Incident(rs.getInt("id"), rs.getInt("monitor_run_id"), rs.getTimestamp("down_time").getTime(), time, rs.getInt("status_code"), rs.getBoolean("resolved")));
            }
            rs.close();
        }
        return incidents;
    }

    public void updateIncident(Incident incident) throws SQLException {
        String sql = "UPDATE incidents SET resolved=? resolved_time=? WHERE id=?";
        try(Connection connection = DriverManager.getConnection(url, username, password)) {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setBoolean(1, incident.isResolved());
            pst.setTimestamp(2, new Timestamp(incident.getResolvedTime()));
            pst.setInt(3, incident.getId());
        }
    }

    public Incident getLastUnresolvedIncident(Connection connection, int monitorId) throws SQLException {
        String sql = "SELECT i.* FROM incidents i JOIN monitor_runs mr ON mr.id=i.monitor_run_id JOIN monitors m ON m.id=mr.monitor_id WHERE m.id=? AND i.resolved=false";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                Incident incident = new Incident(rs.getInt("id"), rs.getInt("monitor_run_id"), rs.getTimestamp("down_time").getTime(), 0L, rs.getInt("status_code"), rs.getBoolean("resolved"));
                System.out.println("In dao: " + incident);
                return incident;
            }
        }
        return null;
    }

    public Incident getIncidentById(Connection connection, int monitorId) throws SQLException {
        String sql = "SELECT * FROM incidents WHERE id=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new Incident(rs.getInt("id"), rs.getInt("monitor_run_id"), rs.getTimestamp("down_time").getTime(), 0L, rs.getInt("status_code"), rs.getBoolean("resolved"));
            }
        }
        return null;
    }

    public void resolveIncident(Connection connection, int incidentId, String notes) throws SQLException {
        String sql = "UPDATE incidents SET resolved=true, resolved_time=?, notes=? WHERE id=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            pst.setString(2, notes);
            pst.setInt(3, incidentId);
            pst.executeUpdate();
        }
    }
}
