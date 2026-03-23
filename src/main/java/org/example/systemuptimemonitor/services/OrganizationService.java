package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.OrganizationDao;
import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

public class OrganizationService {
    private static final Logger LOG = Logger.getLogger(OrganizationService.class.getName());
    private final static OrganizationDao organizationDao = new OrganizationDao();

    public List<String> getAllOrganizations() throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            return organizationDao.getAllOrganizations(connection);
        }
    }
}
