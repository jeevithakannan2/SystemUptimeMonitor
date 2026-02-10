package org.example.systemuptimemonitor.services;

import org.example.systemuptimemonitor.dao.MonitorRunDao;
import org.example.systemuptimemonitor.model.MonitorRun;
import org.example.systemuptimemonitor.util.DBManager;

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
