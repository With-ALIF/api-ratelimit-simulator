package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.*;

import java.time.Duration;
import java.time.LocalDateTime;

public class FixedWindowPolicy implements RatePolicy {

    private final int maxRequests;
    private final Duration window;

    public FixedWindowPolicy(int maxRequests, Duration window) {
        this.maxRequests = maxRequests;
        this.window = window;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        LocalDateTime now = LocalDateTime.now();

        long count = requestLog.getRequests().stream()
                .filter(r -> Duration.between(r.getTimestamp(), now).compareTo(window) <= 0)
                .count();

        if (count > maxRequests) {
            report.addViolation("Fixed window limit exceeded: " + count + " requests");
            report.setLevel(ViolationLevel.WARNING);
        }
    }
}
