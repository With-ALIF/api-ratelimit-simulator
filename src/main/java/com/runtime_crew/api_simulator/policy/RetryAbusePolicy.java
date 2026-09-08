package com.runtime_crew.api_simulator.policy;

import com.runtime_crew.api_simulator.model.*;

import java.time.Duration;
import java.util.List;

public class RetryAbusePolicy implements RatePolicy {

    private final int maxConsecutiveBlocked;
    private final Duration retryWindow;

    public RetryAbusePolicy(int maxConsecutiveBlocked, Duration retryWindow) {
        this.maxConsecutiveBlocked = maxConsecutiveBlocked;
        this.retryWindow = retryWindow;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        if (requestLog == null) return;
        List<ServiceRequest> requests = requestLog.getRequests();

        if (requests == null || requests.size() < maxConsecutiveBlocked) {
            return;
        }

        detectRapidRetries(requests, report);

        detectSuspiciousRapidRequests(requests, report);
    }

    private void detectRapidRetries(List<ServiceRequest> requests, AbuseReport report) {
        int rapidRetryCount = 0;

        for (int i = 0; i < requests.size() - 1; i++) {
            Duration timeBetween = Duration.between(
                requests.get(i).getTimestamp(),
                requests.get(i + 1).getTimestamp()
            );

            if (timeBetween.toMillis() < 1000) {
                rapidRetryCount++;
            }
        }

        if (rapidRetryCount > 5) {
            report.addViolation(String.format(
                "Retry abuse detected: %d rapid retry attempts (< 1 second apart)",
                rapidRetryCount
            ));
            report.setLevel(ViolationLevel.CRITICAL);
        }
    }

    private void detectSuspiciousRapidRequests(List<ServiceRequest> requests, AbuseReport report) {
        int consecutiveRapid = 0;
        int maxConsecutive = 0;

        for (int i = 0; i < requests.size() - 1; i++) {
            Duration timeBetween = Duration.between(
                requests.get(i).getTimestamp(),
                requests.get(i + 1).getTimestamp()
            );

            if (timeBetween.compareTo(retryWindow) <= 0) {
                consecutiveRapid++;
                maxConsecutive = Math.max(maxConsecutive, consecutiveRapid);
            } else {
                consecutiveRapid = 0;
            }
        }

        if (maxConsecutive >= maxConsecutiveBlocked) {
            report.addViolation(String.format(
                "Excessive consecutive requests: %d requests in quick succession",
                maxConsecutive + 1
            ));

            if (report.getLevel() == ViolationLevel.NORMAL) {
                report.setLevel(ViolationLevel.WARNING);
            }
        }
    }

    @Override
    public String getName() {
        return "Retry Abuse Policy";
    }

    @Override
    public String getDescription() {
        return String.format("Detects rapid retries and %d+ consecutive fast requests", maxConsecutiveBlocked);
    }
}