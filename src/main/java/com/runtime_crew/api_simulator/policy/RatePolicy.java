package com.runtime_crew.api_simulator.policy;

import com.runtime_crew.api_simulator.model.AbuseReport;
import com.runtime_crew.api_simulator.model.Client;
import com.runtime_crew.api_simulator.model.RequestLog;

public interface RatePolicy {

    void evaluate(RequestLog requestLog, AbuseReport report);

    default void evaluate(Client client) {
    }

    default void evaluate(Client client, RequestLog requestLog, AbuseReport report) {
        evaluate(requestLog, report);
    }

    default String getName() {
        return getClass().getSimpleName();
    }

    default String getDescription() {
        return "Rate limit policy";
    }
}
