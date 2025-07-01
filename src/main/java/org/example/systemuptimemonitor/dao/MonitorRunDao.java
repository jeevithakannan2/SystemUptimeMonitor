package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.model.MonitorRun;

import java.sql.*;

public class MonitorRunDao {
    public void createMonitorRun(Connection connection, MonitorRun monitorRun) throws SQLException {
        String sql = "INSERT INTO monitor_runs (monitor_id, time, response_time, status_code, success) VALUES(?,?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setInt(1, monitorRun.getMonitor_id());
            pst.setTimestamp(2, new Timestamp(monitorRun.getTime()));
            pst.setInt(3, monitorRun.getResponse_time());
            pst.setInt(4, monitorRun.getStatus_code());
            pst.setBoolean(5, monitorRun.isSuccess());
            pst.executeUpdate();
            ResultSet generatedId = pst.getGeneratedKeys();
            if (generatedId.next()) monitorRun.setId(generatedId.getInt(1));
        }
    }
}
