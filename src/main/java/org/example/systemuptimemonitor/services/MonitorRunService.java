package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.MonitorRunDao;
import org.example.systemuptimemonitor.model.MonitorRun;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MonitorRunService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    private final MonitorRunDao monitorRunDao = new MonitorRunDao();

    public void createMonitorRun(MonitorRun monitorRun) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            monitorRunDao.createMonitorRun(connection, monitorRun);
        }
    }
}
