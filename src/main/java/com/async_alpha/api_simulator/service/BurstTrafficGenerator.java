package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.RequestType;
import com.async_alpha.api_simulator.model.ServiceRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BurstTrafficGenerator {

    private static final RequestType[] TYPES = RequestType.values();

    public List<ServiceRequest> generateBurst(String clientId, int requestCount, Duration totalDuration) {
        if (clientId == null || clientId.trim().isEmpty()) {
            clientId = "CLIENT_A";
        }
        if (requestCount <= 0) {
            requestCount = 20;
        }

        List<ServiceRequest> burstRequests = new ArrayList<>(requestCount);
        LocalDateTime baseTime = LocalDateTime.now().minus(totalDuration);
        long stepMillis = totalDuration.toMillis() / Math.max(1, requestCount);

        for (int i = 0; i < requestCount; i++) {
            LocalDateTime timestamp = baseTime.plus(Duration.ofMillis(i * stepMillis));
            RequestType type = TYPES[i % TYPES.length];
            burstRequests.add(new ServiceRequest(clientId, type, timestamp));
        }

        return burstRequests;
    }
}
