package org.example.systemuptimemonitor.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OrganizationDao {

    public List<String> getAllOrganizations(Connection conn) throws SQLException {
        String sql = "SELECT name FROM organizations";
        List<String> organizations = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                organizations.add(rs.getString("name"));
            }
        }
        return organizations;
    }
}
