package com.runtime_crew.api_simulator.policy;

import com.runtime_crew.api_simulator.model.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbnormalPatternPolicy implements RatePolicy {

    private final int unusualHourThreshold;

    public AbnormalPatternPolicy(int unusualHourThreshold) {
        this.unusualHourThreshold = unusualHourThreshold;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        if (requestLog == null) return;
        List<ServiceRequest> requests = requestLog.getRequests();

        if (requests == null || requests.isEmpty()) {
            return;
        }

        detectOffHoursActivity(requests, report);

        detectRequestTypeImbalance(requests, report);

        detectUniformIntervals(requests, report);
    }

    private void detectOffHoursActivity(List<ServiceRequest> requests, AbuseReport report) {
        int offHoursCount = 0;

        for (ServiceRequest request : requests) {
            LocalTime time = request.getTimestamp().toLocalTime();
            int hour = time.getHour();

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

        for (ServiceRequest request : requests) {
            typeCounts.merge(request.getRequestType(), 1, Integer::sum);
        }

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
            return;
        }

        long[] intervals = new long[requests.size() - 1];
        for (int i = 0; i < requests.size() - 1; i++) {
            LocalDateTime current = requests.get(i).getTimestamp();
            LocalDateTime next = requests.get(i + 1).getTimestamp();
            intervals[i] = java.time.Duration.between(current, next).getSeconds();
        }

        int uniformCount = 0;
        for (int i = 0; i < intervals.length - 1; i++) {
            if (Math.abs(intervals[i] - intervals[i + 1]) <= 1) {
                uniformCount++;
            }
        }

        double uniformPercentage = (uniformCount * 100.0) / intervals.length;
        if (uniformPercentage > 70 && requests.size() >= 10) {
            report.addViolation(String.format(
                "Bot-like behavior: %.1f%% of requests have uniform intervals (automated script suspected)",
                uniformPercentage
            ));
            report.setLevel(ViolationLevel.CRITICAL);
        }
    }

    @Override
    public String getName() {
        return "Abnormal Pattern Policy";
    }

    @Override
    public String getDescription() {
        return "Detects off-hours activity, type imbalance, and bot-like patterns";
    }
}