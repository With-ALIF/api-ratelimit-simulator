package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;

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

    public String generateUsageReport(String clientId, ClientActivity activity, RequestLog log, AbuseReport abuse) {
        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        int total = activity != null ? activity.getTotalRequests() : 0;
        int allowed = activity != null ? activity.getAllowedRequests() : 0;
        int blocked = activity != null ? activity.getBlockedRequests() : 0;
        double rate = activity != null ? activity.getSuccessRate() : 100.0;
        String severity = abuse != null ? abuse.getLevel().toString() : "NORMAL";

        sb.append("\n");
        sb.append(center("QUICK SUMMARY", W)).append("\n");
        sb.append(center(clientId, W)).append("\n");
        sb.append("\n");
        sb.append(line()).append("\n");
        sb.append("\n");

        sb.append("  CLIENT INFO\n");
        sb.append("  ").append(String.format("%-18s %s", "Name:", clientId)).append("\n");
        sb.append("  ").append(String.format("%-18s %s", "Generated:", dateStr)).append("\n");
        sb.append("  ").append(String.format("%-18s %s %s", "Status:", getSeverityDot(severity), severity)).append("\n");
        sb.append("\n");

        sb.append("  USAGE STATS\n");
        sb.append("  ").append(String.format("%-18s %d", "Total Requests:", total)).append("\n");
        sb.append("  ").append(String.format("%-18s %d  (%.1f%%)", "Allowed:", allowed, total > 0 ? (allowed * 100.0) / total : 100)).append("\n");
        sb.append("  ").append(String.format("%-18s %d  (%.1f%%)", "Blocked:", blocked, total > 0 ? (blocked * 100.0) / total : 0)).append("\n");
        sb.append("\n");
        sb.append("  Success Rate: ").append(String.format("%.1f%%  %s", rate, getProgressBar((int) rate))).append("\n");
        sb.append("\n");

        if (log != null && !log.getRequests().isEmpty()) {
            sb.append("  REQUEST TYPES\n");
            Map<RequestType, Integer> dist = getRequestTypeDistribution(log);
            for (Map.Entry<RequestType, Integer> entry : dist.entrySet()) {
                double pct = (entry.getValue() * 100.0) / total;
                sb.append("  ").append(String.format("%-10s %3d  %s", entry.getKey(), entry.getValue(), getSmallBar((int) pct))).append("\n");
            }
            sb.append("\n");

            sb.append("  TIMELINE\n");
            sb.append("  ").append(String.format("First: %s", log.getRequests().get(0).getTimestamp().format(TIME_FORMAT))).append("\n");
            sb.append("  ").append(String.format("Last:  %s", log.getRequests().get(log.getRequests().size() - 1).getTimestamp().format(TIME_FORMAT))).append("\n");
            if (activity != null) {
                sb.append("  ").append(String.format("Active: %s", activity.getLastActivityTime())).append("\n");
            }
            sb.append("\n");
        }

        if (abuse != null && abuse.hasViolations()) {
            sb.append("  SECURITY ALERTS\n");
            sb.append("  ").append(String.format("%d violation(s) detected", abuse.getViolationCount())).append("\n");
            for (String v : abuse.getViolations()) {
                sb.append("    ").append(getSeverityIcon(severity)).append(" ").append(v).append("\n");
            }
            sb.append("\n");
        }

        sb.append(line()).append("\n");
        sb.append("\n");
        return sb.toString();
    }

    public String generateAllClientsUsageReport(int totalReqs, int totalAllowed, int totalBlocked, double totalRate,
                                                 Map<String, ClientActivity> allActivities) {
        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDateTime.now().format(DATE_TIME_FORMAT);

        sb.append("\n");
        sb.append(center("QUICK SUMMARY - ALL CLIENTS", W)).append("\n");
        sb.append("\n");
        sb.append(line()).append("\n");
        sb.append("\n");

        sb.append("  GENERATED\n");
        sb.append("  ").append(dateStr).append("\n");
        sb.append("\n");

  sb.append("  OVERALL STATS\n");
sb.append("  ").append(String.format("%-18s %5d", "Total Requests:", totalReqs)).append("\n");
sb.append("  ").append(String.format("%-18s %5d  (%5.1f%%)", "Allowed:", totalAllowed,
    totalReqs > 0 ? (totalAllowed * 100.0) / totalReqs : 100)).append("\n");
sb.append("  ").append(String.format("%-18s %5d  (%5.1f%%)", "Blocked:", totalBlocked,
    totalReqs > 0 ? (totalBlocked * 100.0) / totalReqs : 0)).append("\n");
sb.append("\n");
sb.append("  ").append(String.format("%-18s %5.1f%%  %s", "Success Rate:", totalRate,
    getProgressBar((int) totalRate))).append("\n");
sb.append("\n");

        sb.append("  PER CLIENT BREAKDOWN\n");
        sb.append("  ").append(String.format("%-14s %6s %8s %8s %9s", "CLIENT", "TOTAL", "ALLOWED", "BLOCKED", "RATE")).append("\n");
        sb.append("  ").append("-".repeat(50)).append("\n");
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            ClientActivity a = entry.getValue();
            sb.append("  ").append(String.format("%-14s %6d %8d %8d %8.1f%%",
                entry.getKey(), a.getTotalRequests(), a.getAllowedRequests(),
                a.getBlockedRequests(), a.getSuccessRate())).append("\n");
        }
        sb.append("  ").append("-".repeat(50)).append("\n");
        sb.append("  ").append(String.format("%-14s %6d %8d %8d %8.1f%%",
            "TOTAL", totalReqs, totalAllowed, totalBlocked, totalRate)).append("\n");
        sb.append("\n");

        sb.append(line()).append("\n");
        sb.append("\n");
        return sb.toString();
    }

    public String generateComparisonReport(Map<String, ClientActivity> allActivities) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><style>");
        sb.append("body { font-family: 'Segoe UI', sans-serif; background: #090d16; color: #e2e8f0; margin: 20px; }");
        sb.append("h2 { color: #38bdf8; text-align: center; margin-bottom: 20px; }");
        sb.append("table { border-collapse: collapse; width: 100%; margin: 10px 0; }");
        sb.append("th { background: #1e293b; color: #38bdf8; padding: 10px 16px; text-align: left; border-bottom: 2px solid #334155; }");
        sb.append("td { padding: 10px 16px; border-bottom: 1px solid #1e293b; }");
        sb.append("tr:hover td { background: #1e293b; }");
        sb.append(".ok { color: #22c55e; font-weight: bold; }");
        sb.append(".warn { color: #f59e0b; font-weight: bold; }");
        sb.append(".danger { color: #ef4444; font-weight: bold; }");
        sb.append("</style></head><body>");
        sb.append("<h2>MULTI-CLIENT COMPARISON REPORT</h2>");
        sb.append("<table><thead><tr>");
        sb.append("<th>CLIENT</th><th>TOTAL</th><th>ALLOWED</th><th>BLOCKED</th><th>SUCCESS %</th>");
        sb.append("</tr></thead><tbody>");
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            ClientActivity a = entry.getValue();
            double rate = a.getSuccessRate();
            String cssClass = rate >= 80 ? "ok" : rate >= 50 ? "warn" : "danger";
            sb.append(String.format("<tr><td>%s</td><td>%d</td><td>%d</td><td>%d</td><td class=\"%s\">%.1f%%</td></tr>",
                entry.getKey(), a.getTotalRequests(), a.getAllowedRequests(),
                a.getBlockedRequests(), cssClass, rate));
        }
        sb.append("</tbody></table></body></html>");
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
        return getProgressBar(percentage, 10);
    }

    private String getProgressBar(int percentage, int width) {
        int filled = (int) Math.round((percentage * width) / 100.0);
        filled = Math.max(0, Math.min(width, filled));
        int empty = width - filled;
        return "\u2588".repeat(filled) + "\u2591".repeat(empty);
    }

    private String getSmallBar(int percentage) {
        int filled = (int) Math.round(percentage / 10.0);
        int empty = 10 - filled;
        return "[" + "\u2588".repeat(Math.max(0, filled)) + "\u2591".repeat(Math.max(0, empty)) + "]";
    }

    private Map<RequestType, Integer> getRequestTypeDistribution(RequestLog log) {
        Map<RequestType, Integer> distribution = new EnumMap<>(RequestType.class);
        for (ServiceRequest request : log.getRequests()) {
            distribution.merge(request.getRequestType(), 1, Integer::sum);
        }
        return distribution;
    }

    public String generateAllClientsFullReport(Map<String, ClientActivity> allActivities) {
        StringBuilder sb = new StringBuilder();
        String dateStr = LocalDateTime.now().format(DATE_TIME_FORMAT);
        int W2 = 70;

        int grandTotal = 0, grandAllowed = 0, grandBlocked = 0;
        for (ClientActivity a : allActivities.values()) {
            grandTotal += a.getTotalRequests();
            grandAllowed += a.getAllowedRequests();
            grandBlocked += a.getBlockedRequests();
        }
        double grandRate = grandTotal == 0 ? 100.0 : (grandAllowed * 100.0) / grandTotal;
        double grandBlockRate = grandTotal == 0 ? 0.0 : (grandBlocked * 100.0) / grandTotal;
        int avgPerClient = allActivities.isEmpty() ? 0 : grandTotal / allActivities.size();

        String sepDouble = "=".repeat(W2);
        String sepSingle = "-".repeat(W2);

        sb.append(sepDouble).append("\n");
        sb.append("                    ALL CLIENTS - FULL REPORT\n");
        sb.append("                 API RATE-LIMIT & ABUSE SIMULATOR\n");
        sb.append(sepDouble).append("\n\n");
        sb.append("Generated: ").append(dateStr).append("\n\n");

        sb.append("OVERALL\n");
        sb.append(sepSingle).append("\n");
        String overallFmt = " %-14s%-14s%-14s%s\n";
        sb.append(String.format(overallFmt, "Requests", "Allowed", "Blocked", "Success Rate"));
        long grandAllowedPct = Math.round(grandTotal > 0 ? (grandAllowed * 100.0) / grandTotal : 0.0);
        long grandBlockedPct = Math.round(grandTotal > 0 ? (grandBlocked * 100.0) / grandTotal : 0.0);
        String allowedStr = grandAllowed + " (" + grandAllowedPct + "%)";
        String blockedStr = grandBlocked + " (" + grandBlockedPct + "%)";
        sb.append(String.format(overallFmt,
            String.valueOf(grandTotal), allowedStr, blockedStr, String.format("%.1f%%", grandRate)));
        sb.append(sepSingle).append("\n\n");

        sb.append("CLIENT PERFORMANCE\n");
        sb.append(sepSingle).append("\n");
        String rowFmt = " %-13s%-13s%-13s%-13s%-10s%s\n";
        sb.append(String.format(rowFmt, "Client", "Requests", "Allowed", "Blocked", "Success", "Last Act."));
        sb.append(sepSingle).append("\n");
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            String cid = entry.getKey();
            ClientActivity a = entry.getValue();
            long aAllowedPct = Math.round(a.getSuccessRate());
            long aBlockedPct = Math.round(100.0 - a.getSuccessRate());
            String cAllowed = a.getAllowedRequests() + " (" + aAllowedPct + "%)";
            String cBlocked = a.getBlockedRequests() + " (" + aBlockedPct + "%)";
            String lastAct = a.getLastActivityTime() != null ? a.getLastActivityTime() : "-";
            sb.append(String.format(rowFmt,
                cid, String.valueOf(a.getTotalRequests()), cAllowed, cBlocked,
                String.format("%.1f%%", a.getSuccessRate()), lastAct));
        }
        sb.append(sepSingle).append("\n");
        sb.append(String.format(rowFmt,
            "TOTAL", String.valueOf(grandTotal), allowedStr, blockedStr,
            String.format("%.1f%%", grandRate), "-"));
        sb.append(sepSingle).append("\n\n");

        sb.append("RATE-LIMIT STATUS\n");
        sb.append(sepSingle).append("\n");
        sb.append(String.format(" Allowed :  %s  %5.1f%%\n", getProgressBar((int) Math.round(grandRate), 10), grandRate));
        sb.append(String.format(" Blocked :  %s  %5.1f%%\n", getProgressBar((int) Math.round(grandBlockRate), 10), grandBlockRate));
        sb.append(sepSingle).append("\n\n");

        sb.append("SUMMARY\n");
        sb.append(sepSingle).append("\n");
        sb.append(String.format(" %-23s          : %5d\n", "Total Clients", allActivities.size()));
        sb.append(String.format(" %-23s       : %5d\n", "Total Requests", grandTotal));
        sb.append(String.format(" %-23s        : %5d\n", "Total Allowed", grandAllowed));
        sb.append(String.format(" %-23s        :  %5d\n", "Total Blocked", grandBlocked));
        sb.append(String.format(" %-23s    : %5.1f%%\n", "Overall Success Rate", grandRate));
        sb.append(String.format(" %-23s     : %5.1f%%\n", "Overall Block Rate", grandBlockRate));
        sb.append(String.format(" %-23s: %5d\n", "Average Requests/Client", avgPerClient));
        sb.append(sepSingle).append("\n\n");

        sb.append("                         END OF REPORT\n");
        sb.append(sepDouble).append("\n");

        return sb.toString();
    }
}