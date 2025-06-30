package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.model.InviteLink;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InviteLinkService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    final InviteLinkDao inviteLinkDao = new InviteLinkDao();

    public void createInviteLink(InviteLink inviteLink) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            inviteLinkDao.createLink(connection, inviteLink);
        }
    }
}
