package org.example.systemuptimemonitor.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
}
