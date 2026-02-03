package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.policy.RatePolicy;

import java.util.List;

public class RateLimitAnalyzer {

    private final List<RatePolicy> policies;

    public RateLimitAnalyzer(List<RatePolicy> policies) {
        this.policies = policies;
    }

    public AbuseReport analyze(RequestLog log) {
        AbuseReport report = new AbuseReport(log.getClientId());

        for (RatePolicy policy : policies) {
            policy.evaluate(log, report);
        }

        return report;
    }
}
