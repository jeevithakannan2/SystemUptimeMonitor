package org.example.systemuptimemonitor.model;

public class Incident {
    private int id;
    private int monitorRunId;
    private long downTime;
    private long resolvedTime;
    private int statusCode;
    private String notes;

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    private boolean resolved;

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

    public Incident(int id, int monitorRunId, long downTime, long resolvedTime, int statusCode, boolean resolved) {
        this.id = id;
        this.monitorRunId = monitorRunId;
        this.downTime = downTime;
        this.resolvedTime = resolvedTime;
        this.statusCode = statusCode;
        this.resolved = resolved;
    }

    public Incident(int monitorRunId, long downTime, int statusCode) {
        this.monitorRunId = monitorRunId;
        this.downTime = downTime;
        this.statusCode = statusCode;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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
