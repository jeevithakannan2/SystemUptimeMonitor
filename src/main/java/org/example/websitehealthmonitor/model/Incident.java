package org.example.websitehealthmonitor.model;

public class Incident {
    private int id;
    private int monitorId;
    private int monitorRunId;
    private long downTime;
    private long resolvedTime;
    private int statusCode;
    private String notes;
    private String expectedStatusCodes;
    private boolean resolved;

    public Incident(int id, int monitorId, int monitorRunId, long downTime, long resolvedTime, int statusCode, boolean resolved, String expectedStatusCodes) {
        this.id = id;
        this.monitorId = monitorId;
        this.monitorRunId = monitorRunId;
        this.downTime = downTime;
        this.resolvedTime = resolvedTime;
        this.statusCode = statusCode;
        this.resolved = resolved;
        this.expectedStatusCodes = expectedStatusCodes;
    }

    public Incident(int id, int monitorRunId, long downTime, long resolvedTime, int statusCode, boolean resolved) {
        this.id = id;
        this.monitorRunId = monitorRunId;
        this.downTime = downTime;
        this.resolvedTime = resolvedTime;
        this.statusCode = statusCode;
        this.resolved = resolved;
    }

    public Incident(int monitorRunId, int monitorId, long downTime, int statusCode, String expectedStatusCodes) {
        this.monitorRunId = monitorRunId;
        this.monitorId = monitorId;
        this.downTime = downTime;
        this.statusCode = statusCode;
        this.expectedStatusCodes = expectedStatusCodes;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public String toString() {
        return "Incident{" +
                "id=" + id +
                ", monitorRunId=" + monitorRunId +
                ", downTime=" + downTime +
                ", resolvedTime=" + resolvedTime +
                ", statusCode=" + statusCode +
                ", resolved=" + resolved +
                '}';
    }

    public String getExpectedStatusCodes() {
        return expectedStatusCodes;
    }

    public void setExpectedStatusCodes(String expectedStatusCodes) {
        this.expectedStatusCodes = expectedStatusCodes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(int monitorId) {
        this.monitorId = monitorId;
    }

    public int getMonitorRunId() {
        return monitorRunId;
    }

    public void setMonitorRunId(int monitorRunId) {
        this.monitorRunId = monitorRunId;
    }

    public long getDownTime() {
        return downTime;
    }

    public void setDownTime(long downTime) {
        this.downTime = downTime;
    }

    public long getResolvedTime() {
        return resolvedTime;
    }

    public void setResolvedTime(long resolvedTime) {
        this.resolvedTime = resolvedTime;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
