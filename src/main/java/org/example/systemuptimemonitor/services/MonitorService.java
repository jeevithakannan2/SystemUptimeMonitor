package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.IncidentDao;
import org.example.systemuptimemonitor.dao.MonitorAuditDao;
import org.example.systemuptimemonitor.dao.MonitorDao;
import org.example.systemuptimemonitor.dao.StatusCodeDao;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.exceptions.MonitorAlreadyExistsException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.util.DBManager;
import org.example.systemuptimemonitor.util.MonitorExecutor;
import org.example.systemuptimemonitor.util.SchemaManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MonitorService {
    private static final Logger LOG = Logger.getLogger(MonitorService.class.getName());
    private final static MonitorDao monitorDao = new MonitorDao();
    private final static StatusCodeDao statusCodeDao = new StatusCodeDao();
    private final static MonitorAuditDao monitorAuditDao = new MonitorAuditDao();
    private final static IncidentDao incidentDao = new IncidentDao();

    public void createMonitor(Monitor monitor) throws SQLException, MonitorAlreadyExistsException {
        try (Connection connection = DBManager.getConnection(monitor.getOrganization())) {
            try {
                connection.setAutoCommit(false);
                if (monitorDao.getMonitorByURLAndOrg(connection, monitor.getTargetUrl(), monitor.getOrganization()) != null) {
                    throw new MonitorAlreadyExistsException();
                }
                monitorDao.createMonitor(connection, monitor);
                statusCodeDao.addStatusCodes(connection, monitor.getId(), monitor.getStatusCodes());
                monitorAuditDao.createAudit(connection, monitor.getId(), "CREATE");
                MonitorExecutor.addMonitor(monitor);
                connection.commit();
                LOG.info("Monitor created: id=" + monitor.getId() + " name=" + monitor.getName());
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createMonitor: " + monitor.getName(), e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void deleteMonitor(int monitorId, String organization) throws SQLException, MissingMonitorException {
        try (Connection connection = DBManager.getConnection(organization)) {
            try {
                connection.setAutoCommit(false);
                Monitor monitor = monitorDao.getMonitorById(connection, monitorId);
                if (monitor == null) throw new MissingMonitorException();
                statusCodeDao.deleteStatusCodes(connection, monitor.getId());
                monitorDao.deleteMonitor(connection, monitor.getId());
                monitorAuditDao.createAudit(connection, monitor.getId(), "DELETE");
                MonitorExecutor.removeMonitor(monitor.getId(), organization);
                connection.commit();
                LOG.info("Monitor deleted: id=" + monitorId);
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for deleteMonitor: id=" + monitorId, e);
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateMonitor(Monitor monitor) throws SQLException {
        try (Connection connection = DBManager.getConnection(monitor.getOrganization())) {
            try {
                connection.setAutoCommit(false);
                monitorDao.updateMonitor(connection, monitor);
                statusCodeDao.deleteStatusCodes(connection, monitor.getId());
                statusCodeDao.addStatusCodes(connection, monitor.getId(), monitor.getStatusCodes());
                monitorAuditDao.createAudit(connection, monitor.getId(), "UPDATE");
                MonitorExecutor.removeMonitor(monitor.getId(), monitor.getOrganization());
                MonitorExecutor.addMonitor(monitor);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public Monitor getMonitor(int monitorId, String organization) throws SQLException, MissingMonitorException {
        Monitor monitor = null;
        try (Connection connection = DBManager.getConnection(organization)) {
            try {
                connection.setAutoCommit(false);
                monitor = monitorDao.getMonitorById(connection, monitorId);
                if (monitor == null) throw new MissingMonitorException();
                ArrayList<Integer> statusCodes = statusCodeDao.getStatusCodes(connection, monitorId);
                monitor.setStatusCodes(statusCodes);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
        return monitor;
    }

    public ArrayList<Monitor> getAllMonitorsByOrganization(String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            ArrayList<Monitor> monitors = monitorDao.getAllMonitorsByOrganization(connection, organization);
            for (Monitor monitor : monitors) {
                monitor.setStatusCodes(statusCodeDao.getStatusCodes(connection, monitor.getId()));
            }
            return monitors;
        }
    }

    public ArrayList<org.example.systemuptimemonitor.model.MonitorAudit> getMonitorHistory(int monitorId, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            return monitorAuditDao.getAudits(connection, monitorId);
        }
    }

    /**
     * Returns all monitors across all organization schemas.
     */
    public ArrayList<Monitor> getAllMonitors() throws SQLException {
        ArrayList<Monitor> allMonitors = new ArrayList<>();
        List<String> organizations = SchemaManager.getAllOrganizations();
        for (String org : organizations) {
            try (Connection connection = DBManager.getConnection(org)) {
                ArrayList<Monitor> orgMonitors = monitorDao.getAllMonitors(connection);
                for (Monitor monitor : orgMonitors) {
                    monitor.setStatusCodes(statusCodeDao.getStatusCodes(connection, monitor.getId()));
                }
                allMonitors.addAll(orgMonitors);
            }
        }
        return allMonitors;
    }

    public boolean hasUnresolvedIncident(int monitorId, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            Incident incident = incidentDao.getLastUnresolvedIncident(connection, monitorId);
            LOG.fine("hasUnresolvedIncident check: monitorId=" + monitorId + " result=" + (incident == null));
            return incident == null;
        }
    }
}
