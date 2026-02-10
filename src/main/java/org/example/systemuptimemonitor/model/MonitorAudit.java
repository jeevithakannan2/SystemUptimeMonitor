package org.example.systemuptimemonitor.model;

import java.sql.Timestamp;

public class MonitorAudit {
    private int id;
    private int monitorId;
    private String operation;
    private long time;

    public MonitorAudit(int id, int monitorId, String operation, long time) {
        this.id = id;
        this.monitorId = monitorId;
        this.operation = operation;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public int getMonitorId() {
        return monitorId;
    }

    public String getOperation() {
        return operation;
    }

    public long getTime() {
        return time;
    }
}
