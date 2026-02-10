package org.example.websitehealthmonitor.services;

import org.example.websitehealthmonitor.dao.InviteLinkDao;
import org.example.websitehealthmonitor.model.InviteLink;
import org.example.websitehealthmonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class InviteLinkService {
    private final InviteLinkDao inviteLinkDao = new InviteLinkDao();

    public void createInviteLink(InviteLink inviteLink, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            inviteLinkDao.createLink(connection, inviteLink);
        }
    }
}
