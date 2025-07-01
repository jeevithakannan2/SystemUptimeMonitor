package org.example.systemuptimemonitor.model;

public class User {
    private int id;
    private String email;
    private String password;
    private String role;
    private String organization;
    private long loggedIn;

    public User(int id, String email, String password, String role, String organization) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.organization = organization;
    }

    public User(String email, String password, String role, String organization) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.organization = organization;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public long getLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(long loggedIn) {
        this.loggedIn = loggedIn;
    }

}
