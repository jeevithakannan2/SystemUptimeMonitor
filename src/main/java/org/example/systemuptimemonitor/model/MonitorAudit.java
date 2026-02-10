package org.example.systemuptimemonitor.model;

public class MonitorAudit {
    private final int id;
    private final int monitorId;
    private final String operation;
    private final long time;

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
