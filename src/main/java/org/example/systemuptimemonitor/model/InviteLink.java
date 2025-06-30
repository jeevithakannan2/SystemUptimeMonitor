package org.example.systemuptimemonitor.model;

public class InviteLink {
    private int id;
    private int createdBy;
    private long createdTime;
    private boolean expired;
    private String url;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    private String role;

    public InviteLink(int id, int createdBy, long createdTime, boolean expired, String url, String role) {
        this.id = id;
        this.createdBy = createdBy;
        this.createdTime = createdTime;
        this.expired = expired;
        this.url = url;
        this.role = role;
    }

    public InviteLink(int createdBy, long createdTime, boolean expired, String url, String role) {
        this.createdBy = createdBy;
        this.createdTime = createdTime;
        this.expired = expired;
        this.url = url;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public boolean isExpired() {
        return expired;
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
