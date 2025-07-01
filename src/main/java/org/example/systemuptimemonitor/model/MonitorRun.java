package org.example.systemuptimemonitor.model;

public class MonitorRun {
    private int id;
    private int monitor_id;
    private long time;
    private int response_time;
    private int status_code;
    private boolean success;

    public MonitorRun(int id, int monitor_id, long time, int response_time, int status_code, boolean success) {
        this.id = id;
        this.monitor_id = monitor_id;
        this.time = time;
        this.response_time = response_time;
        this.status_code = status_code;
        this.success = success;
    }

    public MonitorRun(int monitor_id, long time, int response_time, int status_code) {
        this.monitor_id = monitor_id;
        this.time = time;
        this.response_time = response_time;
        this.status_code = status_code;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMonitor_id() {
        return monitor_id;
    }

    public void setMonitor_id(int monitor_id) {
        this.monitor_id = monitor_id;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getResponse_time() {
        return response_time;
    }

    public void setResponse_time(int response_time) {
        this.response_time = response_time;
    }

    public int getStatus_code() {
        return status_code;
    }

    public void setStatus_code(int status_code) {
        this.status_code = status_code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
