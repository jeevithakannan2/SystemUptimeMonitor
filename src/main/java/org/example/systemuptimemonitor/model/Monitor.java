package org.example.systemuptimemonitor.model;

import java.util.ArrayList;

public class Monitor {
    private int id;
    private String name;
    private String targetUrl;
    private int checkInterval;
    private String organization;
    private int createdBy;
    private long createdTime;
    private int failureCount;
    private boolean enabled;
    private ArrayList<Integer> statusCodes;

    public Monitor(int id, String name, String targetUrl, int checkInterval, long createdTime, int createdBy, int failureCount,  String organization, boolean enabled) {
        this.id = id;
        this.name = name;
        this.targetUrl = targetUrl;
        this.checkInterval = checkInterval;
        this.createdTime = createdTime;
        this.createdBy = createdBy;
        this.failureCount = failureCount;
        this.organization = organization;
        this.enabled = enabled;
    }

    public Monitor(String name, String targetUrl, int checkInterval, long createdTime, int createdBy, int failureCount,  String organization, boolean enabled) {
        this.name = name;
        this.targetUrl = targetUrl;
        this.checkInterval = checkInterval;
        this.createdTime = createdTime;
        this.createdBy = createdBy;
        this.failureCount = failureCount;
        this.organization = organization;
        this.enabled = enabled;
    }

    public boolean isExpectedStatusCode(int statusCode) {
        return statusCodes.contains(statusCode);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public ArrayList<Integer> getStatusCodes() {
        return statusCodes;
    }

    public void setStatusCodes(ArrayList<Integer> statusCodes) {
        this.statusCodes = statusCodes;
    }
}
