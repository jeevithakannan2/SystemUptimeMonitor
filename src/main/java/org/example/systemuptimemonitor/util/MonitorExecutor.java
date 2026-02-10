package org.example.systemuptimemonitor.util;

import org.example.systemuptimemonitor.model.Monitor;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.services.IncidentService;
import org.example.systemuptimemonitor.services.MonitorRunService;
import org.example.systemuptimemonitor.services.MonitorService;

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
    private final static ConcurrentHashMap<Integer, MonitorMap> monitors = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Integer, ScheduledFuture<?>> runningMonitors = new ConcurrentHashMap<>();
    private final static IncidentService incidentService = new IncidentService();
    private final static MonitorService monitorService = new MonitorService();
    private final static MonitorRunService monitorRunService = new MonitorRunService();

    public static void removeMonitor(int monitorId) {
        monitors.remove(monitorId);
        ScheduledFuture<?> scheduledFuture = runningMonitors.remove(monitorId);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
            LOG.info("Monitor removed from scheduler: id=" + monitorId);
        }
    }

    public static void addMonitor(Monitor monitor) {
        if (!monitor.isEnabled()) return;
        MonitorMap monitorMap = new MonitorMap(monitor);
        monitors.put(monitor.getId(), monitorMap);
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Job(monitorMap), 0, monitor.getCheckInterval(), TimeUnit.SECONDS);
        runningMonitors.put(monitor.getId(), scheduledFuture);
        LOG.info("Monitor added to scheduler: id=" + monitor.getId() + " interval=" + monitor.getCheckInterval() + "s");
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

        MonitorService monitorService = new MonitorService();
        try {
            ArrayList<Monitor> monitors1 = monitorService.getAllMonitors();
            for (Monitor monitor : monitors1) {
                if (monitor.isEnabled())
                    monitors.put(monitor.getId(), new MonitorMap(monitor));
            }

        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Failed to load monitors on startup", e);
            throw new RuntimeException(e);
        }

        for (Map.Entry<Integer, MonitorMap> entry : monitors.entrySet()) {
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

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            MonitorRun monitorRun = new MonitorRun(monitorMap.monitor.getId(), startTime);

            try {
                if (!monitorService.hasUnresolvedIncident(monitorMap.monitor.getId())) return;

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
                monitorRunService.createMonitorRun(monitorRun);
                incidentService.resolveLastIncident(monitorMap.monitor.getId(), startTime);
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
            monitorRunService.createMonitorRun(monitorRun);

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
                incidentService.createIncident(monitorRun, sb.toString());
                monitorMap.failCount.set(monitorMap.monitor.getFailureCount());
            }
        }
    }
}
