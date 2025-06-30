package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.MonitorDao;
import org.example.systemuptimemonitor.dao.StatusCodeDao;
import org.example.systemuptimemonitor.model.Monitor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MonitorService {
    private final static String url = "jdbc:mysql://localhost:3306/sysuptimemonitor";
    private final static String username = "jeevi-si3005";
    private final static String password = "Jeeva@200504";
    private final static MonitorDao monitorDao = new MonitorDao();
    private final static StatusCodeDao statusCodeDao = new StatusCodeDao();

    public void createMonitor(Monitor monitor) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            monitorDao.createMonitor(connection, monitor);
            statusCodeDao.addStatusCodes(connection, monitor.getId(), monitor.getStatusCodes());
        }
    }
}
