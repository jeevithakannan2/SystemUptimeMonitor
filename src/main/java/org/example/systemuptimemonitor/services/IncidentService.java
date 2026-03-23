package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.IncidentDao;
import org.example.systemuptimemonitor.dao.MonitorDao;
import org.example.systemuptimemonitor.dao.MonitorRunDao;
import org.example.systemuptimemonitor.dao.MonitorSubscriptionDao;
import org.example.systemuptimemonitor.exceptions.IncidentAlreadyResolvedException;
import org.example.systemuptimemonitor.exceptions.MissingIncidentException;
import org.example.systemuptimemonitor.exceptions.MissingMonitorException;
import org.example.systemuptimemonitor.model.Incident;
import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.util.DBManager;
import org.example.systemuptimemonitor.util.EmailService;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IncidentService {
    private static final Logger LOG = Logger.getLogger(IncidentService.class.getName());
    private final static IncidentDao incidentDao = new IncidentDao();
    private final static MonitorRunDao monitorRunDao = new MonitorRunDao();
    private final static MonitorDao monitorDao = new MonitorDao();
    private final static MonitorSubscriptionDao subscriptionDao = new MonitorSubscriptionDao();

    public ArrayList<Incident> getAllIncidentsByOrganization(String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            return incidentDao.getIncidentsByOrganization(connection, organization);
        }
    }

    public ArrayList<Incident> getIncidentsByMonitor(int monitorId, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            return incidentDao.getIncidentsByMonitor(connection, monitorId);
        }
    }

    public void createIncident(MonitorRun monitorRun, String expectedStatusCodes, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            try {
                connection.setAutoCommit(false);
                monitorRunDao.createMonitorRun(connection, monitorRun);
                Incident incident = new Incident(monitorRun.getId(), monitorRun.getMonitor_id(), monitorRun.getTime(), monitorRun.getStatus_code(), expectedStatusCodes);
                incidentDao.createIncident(connection, incident);
                connection.commit();
                LOG.info("Incident created for monitor_run_id=" + monitorRun.getId() + " monitor_id=" + monitorRun.getMonitor_id() + " status_code=" + monitorRun.getStatus_code());

                try {
                    List<String> emails = subscriptionDao.getSubscribedEmails(connection, monitorRun.getMonitor_id());
                    if (!emails.isEmpty()) {
                        Monitor monitor = monitorDao.getMonitorById(connection, monitorRun.getMonitor_id());
                        if (monitor != null) {
                            EmailService.sendIncidentCreated(emails, monitor.getName(), monitor.getTargetUrl(),
                                    monitorRun.getStatus_code(), expectedStatusCodes, monitorRun.getTime());
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to send incident-created email notification", e);
                }
            } catch (SQLException e) {
                connection.rollback();
                LOG.log(Level.SEVERE, "Transaction failed for createIncident", e);
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public void resolveLastIncident(int monitorId, long resolvedTime, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            Incident incident = incidentDao.getLastUnresolvedIncident(connection, monitorId);
            if (incident != null) {
                incident.setResolvedTime(resolvedTime);
                incident.setResolved(true);
                incidentDao.updateIncident(connection, incident);
                LOG.info("Auto-resolved incident id=" + incident.getId() + " for monitor_id=" + monitorId);

                try {
                    List<String> emails = subscriptionDao.getSubscribedEmails(connection, monitorId);
                    if (!emails.isEmpty()) {
                        Monitor monitor = monitorDao.getMonitorById(connection, monitorId);
                        if (monitor != null) {
                            EmailService.sendIncidentResolved(emails, monitor.getName(), monitor.getTargetUrl(),
                                    incident.getDownTime(), resolvedTime);
                        }
                    }
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to send incident-resolved email notification", e);
                }
            }
        }
    }

    public void resolveIncident(int incidentId, String notes, String organization) throws SQLException, MissingMonitorException, IncidentAlreadyResolvedException {
        try (Connection connection = DBManager.getConnection(organization)) {
            Incident incident = incidentDao.getIncidentById(connection, incidentId);
            if (incident == null) {
                throw new MissingIncidentException();
            }
            if (incident.isResolved()) {
                throw new IncidentAlreadyResolvedException();
            }
            long resolvedTime = System.currentTimeMillis();
            incidentDao.resolveIncident(connection, incidentId, notes);
            LOG.info("Incident resolved: id=" + incidentId);

            try {
                List<String> emails = subscriptionDao.getSubscribedEmails(connection, incident.getMonitorId());
                if (!emails.isEmpty()) {
                    Monitor monitor = monitorDao.getMonitorById(connection, incident.getMonitorId());
                    if (monitor != null) {
                        EmailService.sendIncidentResolved(emails, monitor.getName(), monitor.getTargetUrl(),
                                incident.getDownTime(), resolvedTime);
                    }
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to send incident-resolved email notification", e);
            }
        }
    }
}
