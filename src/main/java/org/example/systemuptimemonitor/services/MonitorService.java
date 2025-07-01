package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.IncidentDao;
import org.example.systemuptimemonitor.dao.MonitorAuditDao;
import org.example.systemuptimemonitor.dao.MonitorDao;
import org.example.systemuptimemonitor.dao.StatusCodeDao;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.exceptions.MonitorAlreadyExistsException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.util.MonitorExecutor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class MonitorService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    private final static MonitorDao monitorDao = new MonitorDao();
    private final static StatusCodeDao statusCodeDao = new StatusCodeDao();
    private final static MonitorAuditDao monitorAuditDao = new MonitorAuditDao();
    private final static IncidentDao incidentDao = new IncidentDao();

    public void createMonitor(Monitor monitor) throws SQLException, MonitorAlreadyExistsException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
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
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

        }
    }

    public void deleteMonitor(int monitorId) throws SQLException, MissingMonitorException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            try {
                connection.setAutoCommit(false);
                Monitor monitor = monitorDao.getMonitorById(connection, monitorId);
                if (monitor == null) throw new MissingMonitorException();
                statusCodeDao.deleteStatusCodes(connection, monitor.getId());
                monitorDao.deleteMonitor(connection, monitor.getId());
                monitorAuditDao.createAudit(connection, monitor.getId(), "DELETE");
                MonitorExecutor.removeMonitor(monitor.getId());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void updateMonitor(Monitor monitor) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            try {
                connection.setAutoCommit(false);
                monitorDao.updateMonitor(connection, monitor);
                statusCodeDao.deleteStatusCodes(connection, monitor.getId());
                statusCodeDao.addStatusCodes(connection, monitor.getId(), monitor.getStatusCodes());
                monitorAuditDao.createAudit(connection, monitor.getId(), "UPDATE");
                MonitorExecutor.removeMonitor(monitor.getId());
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

    public Monitor getMonitor(int monitorId) throws SQLException, MissingMonitorException {
        Monitor monitor = null;
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
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
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return monitorDao.getAllMonitorsByOrganization(connection, organization);
        }
    }

    public ArrayList<Monitor> getAllMonitors() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            ArrayList<Monitor> monitors = monitorDao.getAllMonitors(connection);
            for (Monitor monitor: monitors) {
                monitor.setStatusCodes(statusCodeDao.getStatusCodes(connection, monitor.getId()));
            }
            return monitors;
        }
    }

    public boolean hasUnresolvedIncident(int monitorId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            Incident incident = incidentDao.getLastUnresolvedIncident(connection, monitorId);
            System.out.println("In service: " + incident + " " + monitorId);
            return incident == null;
        }
    }
}
