package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.IncidentDao;
import org.example.systemuptimemonitor.dao.MonitorRunDao;
import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IncidentService {
    private static final Logger LOG = Logger.getLogger(IncidentService.class.getName());
    private final static IncidentDao incidentDao = new IncidentDao();
    private final static MonitorRunDao monitorRunDao = new MonitorRunDao();

    public ArrayList<Incident> getAllIncidentsByOrganization(String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            return incidentDao.getIncidentsByOrganization(connection, organization);
        }
    }

    public ArrayList<Incident> getIncidentsByMonitor(int monitorId) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            return incidentDao.getIncidentsByMonitor(connection, monitorId);
        }
    }

    public void createIncident(MonitorRun monitorRun, String expectedStatusCodes) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            try {
                connection.setAutoCommit(false);
                monitorRunDao.createMonitorRun(connection, monitorRun);
                Incident incident = new Incident(monitorRun.getMonitor_id(), monitorRun.getTime(), monitorRun.getStatus_code(), expectedStatusCodes);
                incidentDao.createIncident(connection, incident);
                connection.commit();
                LOG.info("Incident created for monitor_run_id=" + monitorRun.getId() + " monitor_id=" + monitorRun.getMonitor_id() + " status_code=" + monitorRun.getStatus_code());
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createIncident", e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void resolveLastIncident(int monitorId, long resolvedTime) throws SQLException {
        try (Connection connection = DBManager.getConnection()) {
            Incident incident = incidentDao.getLastUnresolvedIncident(connection, monitorId);
            if (incident != null) {
                incident.setResolvedTime(resolvedTime);
                incident.setResolved(true);
                incidentDao.updateIncident(incident);
                LOG.info("Auto-resolved incident id=" + incident.getId() + " for monitor_id=" + monitorId);
            }
        }
    }

    public void resolveIncident(int incidentId, String notes) throws SQLException, MissingMonitorException, IncidentAlreadyResolvedException {
        try (Connection connection = DBManager.getConnection()) {
            Incident incident = incidentDao.getIncidentById(connection, incidentId);
            if (incident == null) {
                throw new MissingIncidentException();
            }
            if (incident.isResolved()) {
                throw new IncidentAlreadyResolvedException();
            }
            incidentDao.resolveIncident(connection, incidentId, notes);
            LOG.info("Incident resolved: id=" + incidentId);
        }
    }
}
