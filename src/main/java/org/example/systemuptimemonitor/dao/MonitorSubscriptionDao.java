package org.example.systemuptimemonitor.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MonitorSubscriptionDao {

    public void subscribe(Connection conn, int userId, int monitorId) throws SQLException {
        String sql = "INSERT INTO monitor_subscriptions (user_id, monitor_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, monitorId);
            ps.executeUpdate();
        }
    }

    public void unsubscribe(Connection conn, int userId, int monitorId) throws SQLException {
        String sql = "DELETE FROM monitor_subscriptions WHERE user_id = ? AND monitor_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, monitorId);
            ps.executeUpdate();
        }
    }

    public List<String> getSubscribedEmails(Connection conn, int monitorId) throws SQLException {
        String sql = "SELECT u.email FROM users u "
                   + "JOIN monitor_subscriptions ms ON u.id = ms.user_id "
                   + "WHERE ms.monitor_id = ? AND u.email_notifications = true";
        List<String> emails = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, monitorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString("email"));
                }
            }
        }
        return emails;
    }

    public List<Integer> getSubscribedMonitorIds(Connection conn, int userId) throws SQLException {
        String sql = "SELECT monitor_id FROM monitor_subscriptions WHERE user_id = ?";
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("monitor_id"));
                }
            }
        }
        return ids;
    }
}
