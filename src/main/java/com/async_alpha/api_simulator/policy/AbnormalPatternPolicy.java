package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects abnormal usage patterns:
 * - Requests at unusual hours (e.g., 2-5 AM)
 * - Repeated identical request types
 * - Suspicious request distribution
 */
public class AbnormalPatternPolicy implements RatePolicy {

    private final int unusualHourThreshold; // Max requests allowed during off-hours

    public AbnormalPatternPolicy(int unusualHourThreshold) {
        this.unusualHourThreshold = unusualHourThreshold;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        List<ServiceRequest> requests = requestLog.getRequests();
        
        if (requests.isEmpty()) {
            return;
        }

        // Pattern 1: Off-hours activity (2 AM - 5 AM)
        detectOffHoursActivity(requests, report);
        
        // Pattern 2: Request type imbalance
        detectRequestTypeImbalance(requests, report);
        
        // Pattern 3: Uniform time intervals (bot-like behavior)
        detectUniformIntervals(requests, report);
    }

    private void detectOffHoursActivity(List<ServiceRequest> requests, AbuseReport report) {
        int offHoursCount = 0;
        
        for (ServiceRequest request : requests) {
            LocalTime time = request.getTimestamp().toLocalTime();
            int hour = time.getHour();
            
            // Define off-hours as 2 AM - 5 AM
            if (hour >= 2 && hour < 5) {
                offHoursCount++;
            }
        }
        
        if (offHoursCount > unusualHourThreshold) {
            report.addViolation(String.format(
                "Unusual activity: %d requests during off-hours (2-5 AM)",
                offHoursCount
            ));
            
            if (report.getLevel() == ViolationLevel.NORMAL) {
                report.setLevel(ViolationLevel.WARNING);
            }
        }
    }

    private void detectRequestTypeImbalance(List<ServiceRequest> requests, AbuseReport report) {
        Map<RequestType, Integer> typeCounts = new HashMap<>();
        
        // Count each request type
        for (ServiceRequest request : requests) {
            typeCounts.merge(request.getRequestType(), 1, Integer::sum);
        }
        
        // Check for extreme imbalance (>90% of one type)
        int totalRequests = requests.size();
        for (Map.Entry<RequestType, Integer> entry : typeCounts.entrySet()) {
            double percentage = (entry.getValue() * 100.0) / totalRequests;
            
            if (percentage > 90 && totalRequests >= 10) {
                report.addViolation(String.format(
                    "Request type imbalance: %.1f%% are %s requests (potential scraping)",
                    percentage,
                    entry.getKey()
                ));
                
                if (report.getLevel() == ViolationLevel.NORMAL) {
                    report.setLevel(ViolationLevel.WARNING);
                }
                break;
            }
        }
    }

    private void detectUniformIntervals(List<ServiceRequest> requests, AbuseReport report) {
        if (requests.size() < 5) {
            return; // Need at least 5 requests to detect pattern
        }
        
        // Calculate intervals between consecutive requests
        long[] intervals = new long[requests.size() - 1];
        for (int i = 0; i < requests.size() - 1; i++) {
            LocalDateTime current = requests.get(i).getTimestamp();
            LocalDateTime next = requests.get(i + 1).getTimestamp();
            intervals[i] = java.time.Duration.between(current, next).getSeconds();
        }
        
        // Check if intervals are suspiciously uniform (bot-like)
        int uniformCount = 0;
        for (int i = 0; i < intervals.length - 1; i++) {
            // If consecutive intervals differ by less than 1 second
            if (Math.abs(intervals[i] - intervals[i + 1]) <= 1) {
                uniformCount++;
            }
        }
        
        // If >70% of intervals are uniform, it's suspicious
        double uniformPercentage = (uniformCount * 100.0) / intervals.length;
        if (uniformPercentage > 70 && requests.size() >= 10) {
            report.addViolation(String.format(
                "Bot-like behavior: %.1f%% of requests have uniform intervals (automated script suspected)",
                uniformPercentage
            ));
            report.setLevel(ViolationLevel.CRITICAL);
        }
    }
}