package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.model.InviteLink;

import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class InviteLinkService {
    private final InviteLinkDao inviteLinkDao = new InviteLinkDao();

    public void createInviteLink(InviteLink inviteLink) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            inviteLinkDao.createLink(connection, inviteLink);
        }
    }
}
