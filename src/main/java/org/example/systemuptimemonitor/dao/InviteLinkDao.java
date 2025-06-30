package org.example.systemuptimemonitor.dao;

import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.model.InviteLink;

import java.sql.*;

public class InviteLinkDao {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";

    public void createLink(Connection connection, InviteLink inviteLink) throws SQLException {
        String sql = "INSERT INTO invites (created_by, created_time, expired, url, role) VALUES (?,?,?,?,?)";
        try (PreparedStatement pst = connection.prepareStatement(sql);){
            pst.setInt(1, inviteLink.getCreatedBy());
            pst.setTimestamp(2, new Timestamp(inviteLink.getCreatedTime()));
            pst.setBoolean(3, inviteLink.isExpired());
            pst.setString(4, inviteLink.getUrl());
            pst.setString(5, inviteLink.getRole());
            pst.execute();
        }
    }

    public InviteLink getInviteLink(String code) throws SQLException {
        String sql = "SELECT * FROM invites where url=?";
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            PreparedStatement pst = connection.prepareStatement(sql);
            pst.setString(1, code);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return new InviteLink(rs.getInt(1), rs.getInt(2), rs.getTimestamp(3).getTime(), rs.getBoolean(4), rs.getString(5), rs.getString(6));
        }
        return null;
    }

    public void expireInviteLink(Connection connection, String url) throws SQLException {
        String sql = "UPDATE invites SET expired = true WHERE url=?";
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            pst.setString(1, url);
            pst.executeUpdate();
        }
    }
}
