package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class BurstDetectionPolicy implements RatePolicy {

    private final int burstThreshold;  // Number of requests in burst window
    private final Duration burstWindow; // Time window to detect bursts (e.g., 3 seconds)

    public BurstDetectionPolicy(int burstThreshold, Duration burstWindow) {
        this.burstThreshold = burstThreshold;
        this.burstWindow = burstWindow;
    }

    @Override
    public void evaluate(RequestLog requestLog, AbuseReport report) {
        List<ServiceRequest> requests = requestLog.getRequests();
        
        if (requests.isEmpty()) {
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
                    "Burst detected: %d requests in %d seconds at %s",
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
}