package com.runtime_crew.api_simulator.policy;

import com.runtime_crew.api_simulator.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class FixedWindowPolicy implements RatePolicy {

    private final int maxRequests;
    private final Duration window;

    public FixedWindowPolicy(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        if (requestLog == null || requestLog.getRequests().isEmpty()) {
            return;
        }

        List<ServiceRequest> requests = requestLog.getRequests();
        LocalDateTime latestTime = requests.get(requests.size() - 1).getTimestamp();

        long count = requests.stream()
                .filter(r -> {
                    Duration diff = Duration.between(r.getTimestamp(), latestTime);
                    return !diff.isNegative() && diff.compareTo(window) <= 0;
                })
                .count();

        if (count > maxRequests) {
            report.addViolation(String.format(
                "Fixed window limit exceeded: %d requests in %ds window (limit: %d)",
                count, window.getSeconds(), maxRequests));
            report.setLevel(ViolationLevel.WARNING);
        }
    }

    @Override
    public String getName() {
        return "Fixed Window Policy";
    }

    @Override
    public String getDescription() {
        return String.format("Max %d requests per %ds window", maxRequests, window.getSeconds());
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getWindow() {
        return window;
    }
}