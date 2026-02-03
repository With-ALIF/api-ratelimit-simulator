package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.AbuseReport;

public class ReportGenerator {

    public String generate(AbuseReport report) {
        return """
                Client ID: %s
                Severity: %s
                Violations:
                %s
                """.formatted(
                report.getClientId(),
                report.getLevel(),
                report.getViolations()
        );
    }
}
