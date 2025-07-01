package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.InviteLinkDao;
import org.example.systemuptimemonitor.dao.UserDao;
import org.example.systemuptimemonitor.exceptions.InviteLinkExpiredException;
import org.example.systemuptimemonitor.exceptions.RoleMissingException;
import org.example.systemuptimemonitor.exceptions.UserAlreadyExistsException;
import org.example.systemuptimemonitor.model.InviteLink;
import org.example.systemuptimemonitor.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UserService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    private final static UserDao userDao = new UserDao();
    private final static InviteLinkDao inviteLinkDao = new InviteLinkDao();

    public void createUser(User user) throws SQLException, RoleMissingException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
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
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void createUserFromLink(User user, InviteLink inviteLink) throws SQLException, InviteLinkExpiredException, UserAlreadyExistsException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
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
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteUser(String email) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            userDao.deleteUser(connection, email);
        }
    }
}
