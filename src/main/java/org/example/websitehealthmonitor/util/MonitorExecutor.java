package org.example.websitehealthmonitor.util;

import org.example.websitehealthmonitor.model.Monitor;
import org.example.websitehealthmonitor.model.MonitorRun;
import org.example.websitehealthmonitor.services.IncidentService;
import org.example.websitehealthmonitor.services.MonitorRunService;
import org.example.websitehealthmonitor.services.MonitorService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebListener
public class MonitorExecutor implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(MonitorExecutor.class.getName());
    private final static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    /**
     * Monitors and scheduled futures are keyed by "org:monitorId" to avoid
     * collisions between orgs that may share the same SERIAL-generated IDs.
     */
    private final static ConcurrentHashMap<String, MonitorMap> monitors = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, ScheduledFuture<?>> runningMonitors = new ConcurrentHashMap<>();
    private final static IncidentService incidentService = new IncidentService();
    private final static MonitorService monitorService = new MonitorService();
    private final static MonitorRunService monitorRunService = new MonitorRunService();

    /** Composite key: "organization:monitorId" */
    private static String monitorKey(int monitorId, String organization) {
        return organization + ":" + monitorId;
    }

    public static void removeMonitor(int monitorId, String organization) {
        String key = monitorKey(monitorId, organization);
        monitors.remove(key);
        ScheduledFuture<?> scheduledFuture = runningMonitors.remove(key);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            LOG.info("Monitor removed from scheduler: id=" + monitorId + " org=" + organization);
        }
    }

    public static void addMonitor(Monitor monitor) {
        if (!monitor.isEnabled()) return;
        String key = monitorKey(monitor.getId(), monitor.getOrganization());
        MonitorMap monitorMap = new MonitorMap(monitor);
        monitors.put(key, monitorMap);
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Job(monitorMap), 0, monitor.getCheckInterval(), TimeUnit.SECONDS);
        runningMonitors.put(key, scheduledFuture);
        LOG.info("Monitor added to scheduler: id=" + monitor.getId() + " org=" + monitor.getOrganization() + " interval=" + monitor.getCheckInterval() + "s");
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("MonitorExecutor starting up...");
        try {
            DBManager.initSchema();
            LOG.info("Database schema initialized successfully");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }

        try {
            ArrayList<Monitor> allMonitors = monitorService.getAllMonitors();
            for (Monitor monitor : allMonitors) {
                if (monitor.isEnabled()) {
                    String key = monitorKey(monitor.getId(), monitor.getOrganization());
                    monitors.put(key, new MonitorMap(monitor));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load monitors on startup", e);
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, MonitorMap> entry : monitors.entrySet()) {
            ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Job(entry.getValue()), 0, entry.getValue().monitor.getCheckInterval(), TimeUnit.SECONDS);
            runningMonitors.put(entry.getKey(), scheduledFuture);
        }
        LOG.info("MonitorExecutor started with " + monitors.size() + " active monitors");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("MonitorExecutor shutting down...");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(100, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private static class MonitorMap {
        Monitor monitor;
        AtomicInteger failCount;

        MonitorMap(Monitor monitor) {
            this.monitor = monitor;
            this.failCount = new AtomicInteger(monitor.getFailureCount());
        }
    }

    private static class Job implements Runnable {
        private final MonitorMap monitorMap;

        Job(MonitorMap monitorMap) {
            this.monitorMap = monitorMap;
        }

        private String org() {
            return monitorMap.monitor.getOrganization();
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            MonitorRun monitorRun = new MonitorRun(monitorMap.monitor.getId(), startTime);

            try {
                if (!monitorService.hasUnresolvedIncident(monitorMap.monitor.getId(), org())) return;

                URL url = new URL(monitorMap.monitor.getTargetUrl());

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                int statusCode = connection.getResponseCode();
                long endTime = System.currentTimeMillis();

                monitorRun.setResponse_time((int) (endTime - startTime));
                monitorRun.setStatus_code(statusCode);

                if (!monitorMap.monitor.isExpectedStatusCode(statusCode)) {
                    handleFailure(monitorRun, statusCode);
                    return;
                }

                monitorRun.setSuccess(true);
                monitorMap.failCount.set(monitorMap.monitor.getFailureCount());
                monitorRunService.createMonitorRun(monitorRun, org());
                incidentService.resolveLastIncident(monitorMap.monitor.getId(), startTime, org());
                LOG.fine("Monitor id=" + monitorMap.monitor.getId() + " check OK: status=" + statusCode + " responseTime=" + (endTime - startTime) + "ms");

            } catch (IOException e) {
                LOG.log(Level.WARNING, "Monitor check failed for id=" + monitorMap.monitor.getId() + " url=" + monitorMap.monitor.getTargetUrl());
                monitorRun = new MonitorRun(monitorMap.monitor.getId(), startTime, 0, 0);
                try {
                    handleFailure(monitorRun, 0);
                } catch (SQLException ex) {
                    LOG.log(Level.SEVERE, "Failed to handle monitor failure for id=" + monitorMap.monitor.getId(), ex);
                }
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "Monitor check failed for id=" + monitorMap.monitor.getId() + " url=" + monitorMap.monitor.getTargetUrl(), e);
            }
        }

        private void handleFailure(MonitorRun monitorRun, int statusCode) throws SQLException {
            monitorRun.setSuccess(false);
            monitorRunService.createMonitorRun(monitorRun, org());

            int currentFailures = monitorMap.failCount.get();
            if (currentFailures > 0) {
                currentFailures = monitorMap.failCount.decrementAndGet();
            }

            LOG.warning("Monitor id=" + monitorMap.monitor.getId() + " unexpected status: " + statusCode + " (remaining failures: " + currentFailures + ")");

            if (currentFailures == 0) {
                LOG.severe("Monitor id=" + monitorMap.monitor.getId() + " failure threshold reached - creating incident");
                StringBuilder sb = new StringBuilder();
                ArrayList<Integer> codes = monitorMap.monitor.getStatusCodes();
                if (codes != null) {
                    for (int i = 0; i < codes.size(); i++) {
                        sb.append(codes.get(i));
                        if (i < codes.size() - 1) sb.append(", ");
                    }
                }
                incidentService.createIncident(monitorRun, sb.toString(), org());
                monitorMap.failCount.set(monitorMap.monitor.getFailureCount());
            }
        }
    }
}
