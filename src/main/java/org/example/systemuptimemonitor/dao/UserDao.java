package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.exceptions.MissingUserException;
import org.example.systemuptimemonitor.model.User;

import java.sql.*;

public class UserDao {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";

    public void createUser(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users(email, password, role, organization) VALUES(?,?,?,?)";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getPassword());
            pst.setString(3, user.getRole());
            pst.setString(4, user.getOrganization());
            pst.execute();
        }
    }

    public boolean doesOrganizationExists(Connection connection, String organization) throws SQLException {
        String sql = "SELECT * FROM users where organization=?";
        ResultSet rs = null;
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, organization);
            rs = pst.executeQuery();
            return rs.next();
        } finally {
            if(rs != null) rs.close();
        }
    }

    public User getUserByEmail(String email) throws MissingUserException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            String sql = "SELECT * FROM users WHERE email=?";
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
            } else throw new MissingUserException();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteUser(Connection connection, String email) throws SQLException {
        String sql = "DELETE FROM users WHERE id=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, email);
            pst.executeUpdate();
        }
    }

    public boolean userExists(Connection connection, String email) throws SQLException {
        String sql = "SELECT * FROM users where email=?";
        try(PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }
}
