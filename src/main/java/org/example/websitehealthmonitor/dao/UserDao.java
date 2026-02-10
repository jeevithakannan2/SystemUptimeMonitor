package org.example.websitehealthmonitor.dao;

import org.example.websitehealthmonitor.exceptions.MissingUserException;
import org.example.websitehealthmonitor.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDao {
    private static final Logger LOG = Logger.getLogger(UserDao.class.getName());

    public void createUser(Connection connection, User user) throws SQLException {
        String sql = "INSERT INTO users(email, password, role, organization) VALUES(?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, user.getEmail());
            pst.setString(2, user.getPassword());
            pst.setString(3, user.getRole());
            pst.setString(4, user.getOrganization());
            pst.execute();
        }
    }

    public User getUserByEmail(Connection connection, String email) throws MissingUserException {
        String sql = "SELECT * FROM users WHERE email=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5));
            } else {
                throw new MissingUserException();
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Database error looking up user: " + email, e);
        }
        return null;
    }

    public void deleteUser(Connection connection, String email) throws SQLException {
        String sql = "DELETE FROM users WHERE email=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, email);
            pst.executeUpdate();
        }
    }

    public boolean userExists(Connection connection, String email) throws SQLException {
        String sql = "SELECT * FROM users where email=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, email);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        }
    }
}
