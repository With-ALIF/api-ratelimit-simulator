package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.AbuseReport;

public class ReportGenerator {

    public String generate(AbuseReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Client ID: ").append(report.getClientId()).append("\n");
        sb.append("Severity: ").append(report.getLevel()).append("\n");
        sb.append("Violations:\n");

        if (report.getViolations().isEmpty()) {
            sb.append("  No violations detected\n");
        } else {
            for (String violation : report.getViolations()) {
                sb.append("  - ").append(violation).append("\n");
            }
        }
        return sb.toString();
    }
}
