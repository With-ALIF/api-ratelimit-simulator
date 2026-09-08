package com.runtime_crew.api_simulator.policy;

import com.runtime_crew.api_simulator.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class BurstDetectionPolicy implements RatePolicy {

    private final int burstThreshold;
    private final Duration burstWindow;

    public BurstDetectionPolicy(int burstThreshold, Duration burstWindow) {
        this.burstThreshold = burstThreshold;
        this.burstWindow = burstWindow;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        if (requestLog == null) return;
        List<ServiceRequest> requests = requestLog.getRequests();

        if (requests == null || requests.isEmpty()) {
            return;
        }

        int burstCount = 0;

        for (int i = 0; i < requests.size(); i++) {
            LocalDateTime startTime = requests.get(i).getTimestamp();
            int count = 1;

            for (int j = i + 1; j < requests.size(); j++) {
                Duration diff = Duration.between(startTime, requests.get(j).getTimestamp());

                if (diff.compareTo(burstWindow) <= 0) {
                    count++;
                } else {
                    break;
                }
            }

            if (count >= burstThreshold) {
                burstCount++;
                report.addViolation(String.format(
                    "Burst detected: %d requests within %d seconds starting at %s",
                    count,
                    burstWindow.getSeconds(),
                    startTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
                ));
            }
        }

        if (burstCount >= 3) {
            report.setLevel(ViolationLevel.CRITICAL);
        } else if (burstCount >= 1) {
            if (report.getLevel() == ViolationLevel.NORMAL) {
                report.setLevel(ViolationLevel.WARNING);
            }
        }
    }

    @Override
    public String getName() {
        return "Burst Detection Policy";
    }

    @Override
    public String getDescription() {
        return String.format("Detects %d+ requests within %ds bursts", burstThreshold, burstWindow.getSeconds());
    }

    public int getBurstThreshold() {
        return burstThreshold;
    }

    public Duration getBurstWindow() {
        return burstWindow;
    }
}