package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.exceptions.RoleMissingException;
import org.example.systemuptimemonitor.exceptions.UserAlreadyExistsException;
import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;
import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {
    private static final Logger LOG = Logger.getLogger(UserService.class.getName());
    private final static UserDao userDao = new UserDao();
    private final static InviteLinkDao inviteLinkDao = new InviteLinkDao();

    public void createUser(User user) throws SQLException, RoleMissingException {
        try (Connection connection = DBManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                String role = user.getRole();
                if (userDao.doesOrganizationExists(connection, user.getOrganization())) {
                    if (role == null || (!role.equals("operator") && !role.equals("viewer"))) {
                        throw new RoleMissingException();
                    }
                } else {
                    user.setRole("admin");
                }
                userDao.createUser(connection, user);
                connection.commit();
                LOG.info("User created: " + user.getEmail() + " role=" + user.getRole() + " org=" + user.getOrganization());
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createUser: " + user.getEmail(), e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void createUserFromLink(User user, InviteLink inviteLink) throws SQLException, InviteLinkExpiredException, UserAlreadyExistsException {
        try (Connection connection = DBManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                if (inviteLink.isExpired()) {
                    throw new InviteLinkExpiredException();
                }
                if (inviteLink.getCreatedTime() + 30_000 < System.currentTimeMillis()) {
                    inviteLinkDao.expireInviteLink(connection, inviteLink.getUrl());
                    throw new InviteLinkExpiredException();
                }
                if (userDao.userExists(connection, user.getEmail())) {
                    throw new UserAlreadyExistsException();
                }
                userDao.createUser(connection, user);
                inviteLinkDao.expireInviteLink(connection, inviteLink.getUrl());
                LOG.info("User created from invite: " + user.getEmail() + " role=" + user.getRole());
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createUserFromLink: " + user.getEmail(), e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteUser(String email) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            userDao.deleteUser(connection, email);
        }
    }
}
