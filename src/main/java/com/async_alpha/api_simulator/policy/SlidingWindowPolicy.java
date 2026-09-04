package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.*;

import java.time.Duration;
import java.util.List;

public class SlidingWindowPolicy implements RatePolicy {

    private final int maxRequests;
    private final Duration window;

    public SlidingWindowPolicy(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        if (requestLog == null) return;
        List<ServiceRequest> requests = requestLog.getRequests();

        if (requests == null || requests.isEmpty()) {
            return;
        }

        for (int i = 0; i < requests.size(); i++) {
            int count = 1;

            for (int j = i + 1; j < requests.size(); j++) {
                Duration diff = Duration.between(
                        requests.get(i).getTimestamp(),
                        requests.get(j).getTimestamp()
                );

                if (diff.compareTo(window) <= 0) {
                    count++;
                }

                if (count > maxRequests) {
                    report.addViolation(String.format(
                        "Sliding window abuse detected: %d requests within %ds window (limit: %d)",
                        count, window.getSeconds(), maxRequests));
                    report.setLevel(ViolationLevel.CRITICAL);
                    return;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Sliding Window Policy";
    }

    @Override
    public String getDescription() {
        return String.format("Max %d requests in any %ds sliding window", maxRequests, window.getSeconds());
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }
}