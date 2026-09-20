package com.runtime_crew.api_simulator.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AbuseReport {

    private final String clientId;
    private ViolationLevel level = ViolationLevel.NORMAL;
    private final List<String> violations = new ArrayList<>();

    public AbuseReport(String clientId) {
        this.clientId = clientId;
    }

    public void addViolation(String message) {
        if (message != null && !message.trim().isEmpty() && !violations.contains(message)) {
            violations.add(message);
        }
    }

    public List<String> getViolations() {
        return Collections.unmodifiableList(violations);
    }

    public String getClientId() {
        return clientId;
    }

    public ViolationLevel getLevel() {
        return level;
    }

    public void setLevel(ViolationLevel newLevel) {
        if (newLevel == null) {
            return;
        }
        if (this.level == ViolationLevel.CRITICAL) {
            return;
        }
        if (this.level == ViolationLevel.WARNING && newLevel == ViolationLevel.NORMAL) {
            return;
        }
        this.level = newLevel;
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    public int getViolationCount() {
        return violations.size();
    }

    public boolean isCritical() {
        return level == ViolationLevel.CRITICAL;
    }

    public boolean isWarningOrAbove() {
        return level == ViolationLevel.WARNING || level == ViolationLevel.CRITICAL;
    }

    public int getRiskScore() {
        if (violations.isEmpty()) return 0;
        int base = violations.size() * 15;
        if (level == ViolationLevel.WARNING) base += 20;
        if (level == ViolationLevel.CRITICAL) base += 40;
        return Math.min(base, 100);
    }

    public String getSummary() {
        if (!hasViolations()) return "No violations";
        return String.join("; ", violations);
    }

    @Override
    public String toString() {
        return "AbuseReport{" +
                "clientId='" + clientId + '\'' +
                ", level=" + level +
                ", violationsCount=" + violations.size() +
                '}';
    }
}