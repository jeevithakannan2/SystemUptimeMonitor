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

@WebListener
public class MonitorExecutor implements ServletContextListener {
    private static class MonitorMap {
        Monitor monitor;
        AtomicInteger failCount;

        MonitorMap (Monitor monitor) {
            this.monitor = monitor;
            this.failCount = new AtomicInteger(monitor.getFailureCount());
        }
    }

    private final static ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final static ConcurrentHashMap<Integer, MonitorMap> monitors = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<Integer, ScheduledFuture<?>> runningMonitors = new ConcurrentHashMap<>();
    private final static IncidentService incidentService = new IncidentService();
    private final static MonitorService monitorService = new MonitorService();
    private final static MonitorRunService monitorRunService = new MonitorRunService();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        MonitorService monitorService = new MonitorService();
        try {
            ArrayList<Monitor> monitors1 = monitorService.getAllMonitors();
            for (Monitor monitor: monitors1) {
                if (monitor.isEnabled())
                    monitors.put(monitor.getId(), new MonitorMap(monitor));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        for (Map.Entry<Integer, MonitorMap> entry: monitors.entrySet()) {
            ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Job(entry.getValue()),0, entry.getValue().monitor.getCheckInterval(), TimeUnit.SECONDS);
            runningMonitors.put(entry.getKey(), scheduledFuture);
        }
//
//        executorService.scheduleAtFixedRate(() -> {
//            System.out.println("Monitors: " + monitors);
//            System.out.println("Running monitors: " + runningMonitors);
//        }, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(100, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private static class Job implements Runnable {
        private final MonitorMap monitorMap;

        Job (MonitorMap monitorMap) {
            this.monitorMap = monitorMap;
        }

        @Override
        public void run() {
            try {
                URL url = new URL(monitorMap.monitor.getTargetUrl());

                long startTime = System.currentTimeMillis();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                int statusCode = connection.getResponseCode();
                long endTime = System.currentTimeMillis();

                MonitorRun monitorRun = new MonitorRun(monitorMap.monitor.getId(), startTime, (int) (endTime - startTime), statusCode);
                if (monitorMap.monitor.isExpectedStatusCode(statusCode)) {
                    monitorRun.setSuccess(true);
                    monitorRunService.createMonitorRun(monitorRun);
                    incidentService.resolveLastIncident(monitorMap.monitor.getId(), startTime);
                } else {
                    monitorRun.setSuccess(false);
                    monitorRunService.createMonitorRun(monitorRun);
                    if (monitorMap.failCount.decrementAndGet() == 0 && monitorService.hasUnresolvedIncident(monitorMap.monitor.getId())) {
                        System.out.println("Monitor: " + monitorMap.monitor.getId());
                        System.out.println(!monitorService.hasUnresolvedIncident(monitorMap.monitor.getId()));
                        incidentService.createIncident(monitorRun);
                        monitorMap.failCount.set(monitorMap.monitor.getFailureCount());
                    }
                }
            } catch (IOException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void removeMonitor(int monitorId) {
        ScheduledFuture<?> scheduledFuture = runningMonitors.get(monitorId);
        scheduledFuture.cancel(false);
        System.out.println("Removed monitor called " + monitorId);
    }

    public static void addMonitor(Monitor monitor) {
        if (!monitor.isEnabled()) return;
        MonitorMap monitorMap = new MonitorMap(monitor);
        monitors.put(monitor.getId(), monitorMap);
        ScheduledFuture<?> scheduledFuture = executorService.scheduleAtFixedRate(new Job(monitorMap), 0, monitor.getCheckInterval(), TimeUnit.SECONDS);
        runningMonitors.put(monitor.getId(), scheduledFuture);
    }
}
