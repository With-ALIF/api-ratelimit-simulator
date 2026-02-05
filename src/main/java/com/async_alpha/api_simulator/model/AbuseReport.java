package com.async_alpha.api_simulator.model;

import java.util.ArrayList;
import java.util.List;

public class AbuseReport {

    private final String clientId;
    private ViolationLevel level = ViolationLevel.NORMAL;
    private final List<String> violations = new ArrayList<>();

    public AbuseReport(String clientId) {
        this.clientId = clientId;
    }

    public void addViolation(String message) {
        violations.add(message);
    }

    public List<String> getViolations() {
        return violations;
    }

    public String getClientId() {
        return clientId;
    }

    public ViolationLevel getLevel() {
        return level;
    }

    public void setLevel(ViolationLevel level) {
        this.level = level;
    }
}