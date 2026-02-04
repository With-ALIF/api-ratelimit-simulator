package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detects retry abuse and rapid sequential failures
 * This catches clients who repeatedly hammer the API despite being blocked
 */
public class RetryAbusePolicy implements RatePolicy {

    private final int maxConsecutiveBlocked; // Max allowed consecutive blocked requests
    private final Duration retryWindow; // Time window to check for retry patterns

    public RetryAbusePolicy(int maxConsecutiveBlocked, Duration retryWindow) {
        this.maxConsecutiveBlocked = maxConsecutiveBlocked;
        this.retryWindow = retryWindow;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        List<ServiceRequest> requests = requestLog.getRequests();
        
        if (requests.size() < maxConsecutiveBlocked) {
            return;
        }

        // Detect rapid retry patterns
        detectRapidRetries(requests, report);
        
        // Note: This is a simplified version - in the real implementation,
        // you'd need access to which requests were blocked
        // For now, we detect suspiciously rapid consecutive requests
        detectSuspiciousRapidRequests(requests, report);
    }

    private void detectRapidRetries(List<ServiceRequest> requests, AbuseReport report) {
        int rapidRetryCount = 0;
        
        for (int i = 0; i < requests.size() - 1; i++) {
            Duration timeBetween = Duration.between(
                requests.get(i).getTimestamp(),
                requests.get(i + 1).getTimestamp()
            );
            
            // Retry within 1 second is suspicious
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
            
            // Consecutive requests within retry window
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
}