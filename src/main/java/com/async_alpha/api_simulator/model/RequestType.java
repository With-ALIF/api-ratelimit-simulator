package com.async_alpha.api_simulator.model;

public enum RequestType {

    READ("Read", "Data fetch or login attempt"),
    WRITE("Write", "New record creation or payment request"),
    UPDATE("Update", "Profile update or settings change"),
    DELETE("Delete", "Resource removal or account deactivation");

    private final String displayName;
    private final String description;

    RequestType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name();
    }
}