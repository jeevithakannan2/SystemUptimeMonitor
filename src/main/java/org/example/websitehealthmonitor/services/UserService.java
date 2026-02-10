package org.example.websitehealthmonitor.services;

import org.example.websitehealthmonitor.dao.InviteLinkDao;
import org.example.websitehealthmonitor.dao.UserDao;
import org.example.websitehealthmonitor.exceptions.InviteLinkExpiredException;
import org.example.websitehealthmonitor.exceptions.RoleMissingException;
import org.example.websitehealthmonitor.exceptions.UserAlreadyExistsException;
import org.example.websitehealthmonitor.model.InviteLink;
import org.example.websitehealthmonitor.model.User;
import org.example.websitehealthmonitor.util.DBManager;
import org.example.websitehealthmonitor.util.SchemaManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserService {
    private static final Logger LOG = Logger.getLogger(UserService.class.getName());
    private final static UserDao userDao = new UserDao();
    private final static InviteLinkDao inviteLinkDao = new InviteLinkDao();

    /**
     * Creates a new user. If the organization doesn't exist yet, the user
     * becomes admin and a new org schema is created. Otherwise the user
     * must specify a valid role (operator/viewer).
     */
    public void createUser(User user) throws SQLException, RoleMissingException {
        String org = user.getOrganization();
        boolean orgExists = SchemaManager.organizationExists(org);

        if (orgExists) {
            String role = user.getRole();
            if (role == null || (!role.equals("operator") && !role.equals("viewer"))) {
                throw new RoleMissingException();
            }
        } else {
            user.setRole("admin");
        }

        // If org doesn't exist, create schema first (uses public connection)
        if (!orgExists) {
            try (Connection pubConn = DBManager.getConnection()) {
                SchemaManager.createOrgSchema(pubConn, org);
            }
        }

        // Create user in the org schema
        try (Connection connection = DBManager.getConnection(org)) {
            try {
                connection.setAutoCommit(false);
                userDao.createUser(connection, user);
                connection.commit();
                LOG.info("User created: " + user.getEmail() + " role=" + user.getRole() + " org=" + org);
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createUser: " + user.getEmail(), e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Creates a user from an invite link. Both the invite and user live
     * in the same org schema.
     */
    public void createUserFromLink(User user, InviteLink inviteLink, String organization) throws SQLException, InviteLinkExpiredException, UserAlreadyExistsException {
        try (Connection connection = DBManager.getConnection(organization)) {
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
                connection.commit();
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

    public void deleteUser(String email, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            userDao.deleteUser(connection, email);
        }
    }
}
