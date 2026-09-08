package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.policy.RatePolicy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class RateLimitAnalyzer {

    private final List<RatePolicy> policies;

    public RateLimitAnalyzer(List<RatePolicy> policies) {
        this.policies = policies;
    }

    public AbuseReport analyze(RequestLog log) {
        AbuseReport report = new AbuseReport(log.getClientId());

        List<ServiceRequest> recentRequests = log.getRequests().stream()
            .filter(r -> Duration.between(r.getTimestamp(), LocalDateTime.now()).compareTo(Duration.ofSeconds(10)) <= 0)
            .collect(Collectors.toList());

        if (recentRequests.isEmpty()) {
            return report;
        }

        RequestLog filteredLog = new RequestLog(log.getClientId());
        recentRequests.forEach(filteredLog::addRequest);

        for (RatePolicy policy : policies) {
            policy.evaluate(filteredLog, report);
        }

        return report;
    }

    public List<RatePolicy> getPolicies() {
        return policies;
    }

    public int getPolicyCount() {
        return policies.size();
    }
}