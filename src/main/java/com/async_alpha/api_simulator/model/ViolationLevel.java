package com.async_alpha.api_simulator.model;

public enum ViolationLevel {

    NORMAL("Normal", "Usage is within acceptable limits"),
    WARNING("Warning", "Unusual patterns detected, monitoring recommended"),
    CRITICAL("Critical", "Severe abuse detected, immediate action required");

    private final String displayName;
    private final String description;

    ViolationLevel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isMoreSevereThan(ViolationLevel other) {
        return this.ordinal() > other.ordinal();
    }

    @Override
    public String toString() {
        return name();
    }
}
