package org.example.systemuptimemonitor.model;

public class Incident {
    private int id;
    private int monitorId;
    private long downTime;
    private long resolvedTime;
    private int statusCode;

    public Incident(int id, int monitorId, long downTime, long resolvedTime, int statusCode) {
        this.id = id;
        this.monitorId = monitorId;
        this.downTime = downTime;
        this.resolvedTime = resolvedTime;
        this.statusCode = statusCode;
    }

    public Incident(int monitorId, long downTime, int statusCode) {
        this.monitorId = monitorId;
        this.downTime = downTime;
        this.statusCode = statusCode;
    }
}
