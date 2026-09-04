package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.service.ClientActivityTracker.ClientActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EnhancedReportGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int W = 68;

    public String generateViolationReport(AbuseReport report, RequestLog log, ClientActivity activity) {
        StringBuilder sb = new StringBuilder();
        String clientId = report.getClientId();
        String dateStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        String severity = report.getLevel().toString();
        int total = activity != null ? activity.getTotalRequests() : 0;
        int allowed = activity != null ? activity.getAllowedRequests() : 0;
        int blocked = activity != null ? activity.getBlockedRequests() : 0;
        double successRate = activity != null ? activity.getSuccessRate() : 100.0;

        sb.append("\n");
        sb.append(pad("", 2)).append(center("API SECURITY REPORT", W - 2)).append("\n");
        sb.append(pad("", 2)).append(center("Full Violation & Usage Analysis", W - 2)).append("\n");
        sb.append("\n");
        sb.append(String.format("  Generated: %-28s [ %s ]\n", dateStr, severity));
        sb.append("\n");

        sb.append("  CLIENT INFORMATION\n");
        sb.append("  " + line() + "\n");
        sb.append("\n");
        sb.append(String.format("  CLIENT ID           %s\n", clientId));
        sb.append(String.format("  REPORT DATE         %s\n", dateStr));
        sb.append(String.format("  SEVERITY            %s %s\n", getSeverityDot(severity), severity));
        sb.append("\n");
        sb.append("  " + line() + "\n");
        sb.append("\n");

        sb.append("  USAGE OVERVIEW\n");
        sb.append("\n");
        sb.append(String.format("  %-18s %s\n", "TOTAL REQUESTS", total));
        sb.append("\n");
        sb.append(String.format("  %-18s %-10s %-14s\n", "ALLOWED", allowed,
            String.format("%.1f%% %s", (allowed * 100.0) / Math.max(1, total),
                total > 0 && allowed == total ? "all passed" : "")));
        sb.append("\n");
        sb.append(String.format("  %-18s %-10s %-14s\n", "BLOCKED", blocked,
            String.format("%.1f%%%s", (blocked * 100.0) / Math.max(1, total),
                blocked > 0 ? " limit exceeded" : "")));
        sb.append("\n\n");

        sb.append("  SUCCESS RATE\n");
        sb.append("\n");
        sb.append(String.format("  %.1f%%   %s\n", successRate, getProgressBar((int) successRate)));
        sb.append("\n");
        if (activity != null) {
            sb.append(String.format("  Last Activity: %s\n", activity.getLastActivityTime()));
        }
        sb.append("\n");

        if (log != null && !log.getRequests().isEmpty()) {
            sb.append("  REQUEST TYPE DISTRIBUTION\n");
            sb.append("\n");
            Map<RequestType, Integer> distribution = getRequestTypeDistribution(log);
            int logTotal = log.getRequests().size();
            for (Map.Entry<RequestType, Integer> entry : distribution.entrySet()) {
                double pct = (entry.getValue() * 100.0) / logTotal;
                sb.append(String.format("  %s\n", entry.getKey()));
                sb.append(String.format("  %-10s %s\n", entry.getValue() + " requests", String.format("%.1f%%", pct)));
                sb.append("  " + getProgressBar((int) pct) + "\n");
                sb.append("\n");
            }
        }

        sb.append("  SECURITY STATUS\n");
        sb.append("  " + line() + "\n");
        sb.append("\n");
        if (report.getViolations().isEmpty()) {
            sb.append(center("NO VIOLATIONS DETECTED", W - 4)).append("\n");
            sb.append("\n");
            sb.append(center("Clean usage pattern", W - 4)).append("\n");
            sb.append(center("All requests are within normal thresholds.", W - 4)).append("\n");
        } else {
            sb.append(center(String.format("%d VIOLATION(S) DETECTED", report.getViolations().size()), W - 4)).append("\n");
            sb.append("\n");
            int num = 1;
            for (String violation : report.getViolations()) {
                sb.append(String.format("    %d. %s\n", num++, violation));
            }
        }
        sb.append("\n");
        sb.append("  " + line() + "\n");
        sb.append("\n");

        sb.append("  RECOMMENDATION\n");
        sb.append("  " + line() + "\n");
        sb.append("\n");
        sb.append(String.format("  %s  %s\n", getSeverityIcon(severity), severity));
        sb.append("\n");
        if (report.getLevel() == ViolationLevel.CRITICAL) {
            sb.append("  Immediate action required! Rate limit threshold breach detected.\n");
            sb.append("  Consider blocking this client temporarily or rotating API credentials.\n");
        } else if (report.getLevel() == ViolationLevel.WARNING) {
            sb.append("  High traffic frequency detected. Monitor client activity.\n");
            sb.append("  Review request patterns for potential abuse.\n");
        } else {
            sb.append("  Usage is within normal operational thresholds.\n");
            sb.append("  No immediate action is required.\n");
        }
        if (activity != null && activity.getSuccessRate() < 50) {
            sb.append("  Low success rate: Check client integration and error handling.\n");
        }
        sb.append("  " + line() + "\n");
        sb.append("\n");

        return sb.toString();
    }

    public String generateUsageReport(String clientId, ClientActivity activity, RequestLog log) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CLIENT USAGE SUMMARY: ").append(clientId).append(" ===\n");
        sb.append("Time: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n");
        if (activity != null) {
            sb.append("Total: ").append(activity.getTotalRequests())
              .append(" | Allowed: ").append(activity.getAllowedRequests())
              .append(" | Blocked: ").append(activity.getBlockedRequests())
              .append(" | Success Rate: ").append(String.format("%.1f%%", activity.getSuccessRate())).append("\n");
        }
        if (log != null && !log.getRequests().isEmpty()) {
            sb.append("First: ").append(log.getRequests().get(0).getTimestamp().format(TIME_FORMAT))
              .append(" | Latest: ").append(log.getRequests().get(log.getRequests().size() - 1).getTimestamp().format(TIME_FORMAT)).append("\n");
        }
        return sb.toString();
    }

    public String generateComparisonReport(Map<String, ClientActivity> allActivities) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append(center("MULTI-CLIENT COMPARISON REPORT", W)).append("\n");
        sb.append("\n");
        sb.append(String.format("  %-12s | %8s | %8s | %8s | %10s\n",
            "CLIENT", "TOTAL", "ALLOWED", "BLOCKED", "SUCCESS %"));
        sb.append("  " + line() + "\n");
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            ClientActivity a = entry.getValue();
            sb.append(String.format("  %-12s | %8d | %8d | %8d | %9.1f%%\n",
                entry.getKey(), a.getTotalRequests(), a.getAllowedRequests(),
                a.getBlockedRequests(), a.getSuccessRate()));
        }
        sb.append("  " + line() + "\n");
        sb.append("\n");
        return sb.toString();
    }

    private String center(String text, int width) {
        if (text.length() >= width) return text;
        int pad = (width - text.length()) / 2;
        return " ".repeat(pad) + text;
    }

    private String pad(String text, int len) {
        if (text.length() >= len) return text;
        return text + " ".repeat(len - text.length());
    }

    private String line() {
        return "-".repeat(W - 2);
    }

    private String getSeverityDot(String severity) {
        return "\u25CF";
    }

    private String getSeverityIcon(String severity) {
        switch (severity) {
            case "CRITICAL": return "\u2716";
            case "WARNING":  return "\u26A0";
            default:         return "\u2714";
        }
    }

    private String getProgressBar(int percentage) {
        int filled = (int) Math.round(percentage / 10.0);
        int empty = 10 - filled;
        return "\u2588".repeat(Math.max(0, filled)) + "\u2591".repeat(Math.max(0, empty));
    }

    private Map<RequestType, Integer> getRequestTypeDistribution(RequestLog log) {
        Map<RequestType, Integer> distribution = new EnumMap<>(RequestType.class);
        for (ServiceRequest request : log.getRequests()) {
            distribution.merge(request.getRequestType(), 1, Integer::sum);
        }
        return distribution;
    }
}
