package org.example.websitehealthmonitor.services;

import org.example.websitehealthmonitor.dao.MonitorRunDao;
import org.example.websitehealthmonitor.model.MonitorRun;
import org.example.websitehealthmonitor.util.DBManager;

import java.sql.Connection;
import java.sql.SQLException;

public class MonitorRunService {
    private final MonitorRunDao monitorRunDao = new MonitorRunDao();

    public void createMonitorRun(MonitorRun monitorRun, String organization) throws SQLException {
        try (Connection connection = DBManager.getConnection(organization)) {
            monitorRunDao.createMonitorRun(connection, monitorRun);
        }
    }
}
