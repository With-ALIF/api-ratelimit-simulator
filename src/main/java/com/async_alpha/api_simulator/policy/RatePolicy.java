package com.async_alpha.api_simulator.policy;

import com.async_alpha.api_simulator.model.AbuseReport;
import com.async_alpha.api_simulator.model.RequestLog;

public interface RatePolicy {

    void evaluate(RequestLog requestLog, AbuseReport report);

    default String getName() {
        return getClass().getSimpleName();
    }

    default String getDescription() {
        return "Rate limit policy";
    }
}
