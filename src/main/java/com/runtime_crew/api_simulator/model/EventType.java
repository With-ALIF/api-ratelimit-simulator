package com.runtime_crew.api_simulator.model;

public enum EventType {
    REQUEST("📨 Request Received"),
    ALLOWED("✅ Allowed"),
    RATE_LIMITED("⏳ Rate Limited"),
    ABUSE_DETECTED("⚠️ Abuse Detected"),
    BLOCKED("⛔ Blocked");

    private final String displayName;

    EventType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
