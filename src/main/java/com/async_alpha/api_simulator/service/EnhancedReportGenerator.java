package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.service.ClientActivityTracker.ClientActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class EnhancedReportGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMAT = 
        DateTimeFormatter.ofPattern("HH:mm:ss");

    public String generateViolationReport(AbuseReport report, RequestLog log, ClientActivity activity) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("           API ABUSE & VIOLATION REPORT                    \n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");
        

        sb.append("CLIENT INFORMATION\n");
        sb.append("─────────────────────────────────────────────────────────\n");
        sb.append(String.format("Client ID:        %s\n", report.getClientId()));
        sb.append(String.format("Report Date:      %s\n", LocalDateTime.now().format(DATE_TIME_FORMAT)));
        sb.append(String.format("Severity Level:   %s %s\n\n", 
            getSeverityIcon(report.getLevel()), 
            report.getLevel()));
        
        if (activity != null) {
            sb.append("USAGE STATISTICS\n");
            sb.append("─────────────────────────────────────────────────────────\n");
            sb.append(String.format("Total Requests:   %d\n", activity.getTotalRequests()));
            sb.append(String.format("Allowed:          %d (%.1f%%)\n", 
                activity.getAllowedRequests(),
                (activity.getAllowedRequests() * 100.0) / Math.max(1, activity.getTotalRequests())));
            sb.append(String.format("Blocked:          %d (%.1f%%)\n", 
                activity.getBlockedRequests(),
                (activity.getBlockedRequests() * 100.0) / Math.max(1, activity.getTotalRequests())));
            sb.append(String.format("Success Rate:     %.1f%%\n", activity.getSuccessRate()));
            sb.append(String.format("Last Activity:    %s\n\n", activity.getLastActivityTime()));
        }
        
        if (log != null && !log.getRequests().isEmpty()) {
            sb.append("REQUEST TYPE DISTRIBUTION\n");
            sb.append("─────────────────────────────────────────────────────────\n");
            Map<RequestType, Integer> distribution = getRequestTypeDistribution(log);
            int total = log.getRequests().size();
            
            for (Map.Entry<RequestType, Integer> entry : distribution.entrySet()) {
                double percentage = (entry.getValue() * 100.0) / total;
                sb.append(String.format("%-10s: %3d requests (%.1f%%) %s\n",
                    entry.getKey(),
                    entry.getValue(),
                    percentage,
                    getBar((int) percentage)));
            }
            sb.append("\n");
        }
        
        if (!report.getViolations().isEmpty()) {
            sb.append("VIOLATIONS DETECTED\n");
            sb.append("─────────────────────────────────────────────────────────\n");
            sb.append(String.format("Total Violations: %d\n\n", report.getViolations().size()));
            
            int violationNum = 1;
            for (String violation : report.getViolations()) {
                sb.append(String.format("[%d] %s %s\n", 
                    violationNum++, 
                    "⚠️",
                    violation));
            }
            sb.append("\n");
        } else {
            sb.append("VIOLATIONS DETECTED\n");
            sb.append("─────────────────────────────────────────────────────────\n");
            sb.append("No violations detected - Clean usage pattern\n\n");
        }
 
        sb.append("RECOMMENDATIONS\n");
        sb.append("─────────────────────────────────────────────────────────\n");
        sb.append(generateRecommendations(report, activity));
        sb.append("\n");
        
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("                    END OF REPORT                          \n");
        sb.append("═══════════════════════════════════════════════════════════\n");
        
        return sb.toString();
    }

    public String generateUsageReport(String clientId, ClientActivity activity, RequestLog log) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("╔════════════════════════════════════════════╗\n");
        sb.append("║        CLIENT USAGE SUMMARY                ║\n");
        sb.append("╚════════════════════════════════════════════╝\n\n");
        
        sb.append(String.format("Client:           %s\n", clientId));
        sb.append(String.format("Report Time:      %s\n\n", 
            LocalDateTime.now().format(TIME_FORMAT)));
        
        if (activity != null) {
            sb.append(String.format("Total Requests:   %d\n", activity.getTotalRequests()));
            sb.append(String.format("Allowed:          %d\n", activity.getAllowedRequests()));
            sb.append(String.format("Blocked:          %d\n", activity.getBlockedRequests()));
            sb.append(String.format("Success Rate:     %.1f%%\n", activity.getSuccessRate()));
        }
        
        if (log != null && !log.getRequests().isEmpty()) {
            sb.append(String.format("\nFirst Request:    %s\n", 
                log.getRequests().get(0).getTimestamp().format(TIME_FORMAT)));
            sb.append(String.format("Latest Request:   %s\n", 
                log.getRequests().get(log.getRequests().size() - 1).getTimestamp().format(TIME_FORMAT)));
        }
        
        sb.append("\n════════════════════════════════════════════\n");
        
        return sb.toString();
    }

    public String generateComparisonReport(Map<String, ClientActivity> allActivities) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("═══════════════════════════════════════════════════════════\n");
        sb.append("              MULTI-CLIENT COMPARISON REPORT               \n");
        sb.append("═══════════════════════════════════════════════════════════\n\n");
        
        sb.append(String.format("%-12s | %8s | %8s | %8s | %10s\n", 
            "CLIENT", "TOTAL", "ALLOWED", "BLOCKED", "SUCCESS %"));
        sb.append("─────────────────────────────────────────────────────────\n");
        
        for (Map.Entry<String, ClientActivity> entry : allActivities.entrySet()) {
            ClientActivity activity = entry.getValue();
            sb.append(String.format("%-12s | %8d | %8d | %8d | %9.1f%%\n",
                entry.getKey(),
                activity.getTotalRequests(),
                activity.getAllowedRequests(),
                activity.getBlockedRequests(),
                activity.getSuccessRate()));
        }
        
        sb.append("═══════════════════════════════════════════════════════════\n");
        
        return sb.toString();
    }
    
    private Map<RequestType, Integer> getRequestTypeDistribution(RequestLog log) {
        Map<RequestType, Integer> distribution = new EnumMap<>(RequestType.class);
        
        for (ServiceRequest request : log.getRequests()) {
            distribution.merge(request.getRequestType(), 1, Integer::sum);
        }
        
        return distribution;
    }

    private String getSeverityIcon(ViolationLevel level) {
        return switch (level) {
            case NORMAL -> "✅";
            case WARNING -> "⚠️";
            case CRITICAL -> "🚨";
        };
    }

    private String getBar(int percentage) {
        int bars = percentage / 10;
        return "█".repeat(Math.max(0, bars));
    }

    private String generateRecommendations(AbuseReport report, ClientActivity activity) {
        StringBuilder sb = new StringBuilder();
        
        if (report.getLevel() == ViolationLevel.CRITICAL) {
            sb.append("   CRITICAL: Immediate action required!\n");
            sb.append("   → Consider blocking this client temporarily\n");
            sb.append("   → Review client's API key and permissions\n");
            sb.append("   → Contact client about usage patterns\n");
        } else if (report.getLevel() == ViolationLevel.WARNING) {
            sb.append("  WARNING: Monitor this client closely\n");
            sb.append("   → Send usage warning notification\n");
            sb.append("   → Review if rate limits need adjustment\n");
            sb.append("   → Track for pattern escalation\n");
        } else {
            sb.append(" NORMAL: No action required\n");
            sb.append("   → Client is using API within acceptable limits\n");
            sb.append("   → Continue standard monitoring\n");
        }
        
        if (activity != null) {
            if (activity.getSuccessRate() < 50) {
                sb.append("\n Low success rate detected:\n");
                sb.append("   → Client may need help with integration\n");
                sb.append("   → Consider providing API usage documentation\n");
            }
        }
        
        return sb.toString();
    }
}