package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.ServiceRequest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BurstTrafficGeneratorTest {

    @Test
    public void testBurstGeneration() {
        BurstTrafficGenerator generator = new BurstTrafficGenerator();
        List<ServiceRequest> requests = generator.generateBurst("CLIENT_A", 20, Duration.ofSeconds(10));

        assertNotNull(requests);
        assertEquals(20, requests.size());

        for (ServiceRequest req : requests) {
            assertEquals("CLIENT_A", req.getClientId());
            assertNotNull(req.getRequestType());
            assertNotNull(req.getTimestamp());
        }

        for (int i = 1; i < requests.size(); i++) {
            assertFalse(requests.get(i).getTimestamp().isBefore(requests.get(i - 1).getTimestamp()));
        }
    }
}
