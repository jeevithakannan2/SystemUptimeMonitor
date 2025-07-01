package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.IncidentDao;
import org.example.systemuptimemonitor.dao.MonitorRunDao;
import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.MonitorRun;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

public class IncidentService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    private final static IncidentDao incidentDao = new IncidentDao();
    private final static MonitorRunDao monitorRunDao = new MonitorRunDao();

    public ArrayList<Incident> getAllIncidentsByOrganization(String organization) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return incidentDao.getIncidentsByOrganization(connection, organization);
        }
    }

    public ArrayList<Incident> getIncidentsByMonitor(int monitorId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return incidentDao.getIncidentsByMonitor(connection, monitorId);
        }
    }

    public void createIncident(MonitorRun monitorRun) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            try {
                connection.setAutoCommit(false);
                monitorRunDao.createMonitorRun(connection, monitorRun);
                Incident incident = new Incident(monitorRun.getMonitor_id(), monitorRun.getTime(), monitorRun.getStatus_code());
                incidentDao.createIncident(connection, incident);
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void resolveLastIncident(int monitorId, long resolvedTime) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            Incident incident = incidentDao.getLastUnresolvedIncident(connection, monitorId);
            if (incident != null) {
                incident.setResolvedTime(resolvedTime);
                incident.setResolved(true);
                incidentDao.updateIncident(incident);
            }
        }
    }

    public void resolveIncident(int incidentId, String notes) throws SQLException, MissingMonitorException, IncidentAlreadyResolvedException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            Incident incident = incidentDao.getIncidentById(connection, incidentId);
            if (incident == null) {
                throw new MissingIncidentException();
            }
            if (incident.isResolved()) {
                throw new IncidentAlreadyResolvedException();
            }
            incidentDao.resolveIncident(connection, incidentId, notes);
        }
    }
}
