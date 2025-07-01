package org.example.systemuptimemonitor.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class StatusCodeDao {
    public void addStatusCodes(Connection connection, int monitorId, ArrayList<Integer> statusCodes) throws SQLException {
        String sql = "INSERT INTO status_codes VALUES(?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            for(int statusCode: statusCodes) {
                pst.setInt(1, monitorId);
                pst.setInt(2, statusCode);
                pst.addBatch();
            }
            pst.executeBatch();
        }
    }

    public ArrayList<Integer> getStatusCodes(Connection connection, int monitorId) throws SQLException {
        ArrayList<Integer> statusCodes = new ArrayList<>();
        String sql = "SELECT status_code from status_codes WHERE monitor_id=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setInt(1, monitorId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) statusCodes.add(rs.getInt(1));
        }
        return statusCodes;
    }

    public void deleteStatusCodes(Connection connection, int monitorId) throws SQLException {
        try (PreparedStatement pst = connection.prepareStatement("DELETE FROM status_codes WHERE monitor_id=?")) {
            pst.setInt(1, monitorId);
            pst.executeUpdate();
        }
    }
}
