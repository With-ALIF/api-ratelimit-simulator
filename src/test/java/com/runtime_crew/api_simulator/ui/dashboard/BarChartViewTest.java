package com.runtime_crew.api_simulator.ui.dashboard;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.model.ServiceRequest;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator;
import com.runtime_crew.api_simulator.policy.FixedWindowPolicy;
import com.runtime_crew.api_simulator.service.ClientActivityTracker;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ActivityRecord;
import com.runtime_crew.api_simulator.service.RateLimitAnalyzer;
import com.runtime_crew.api_simulator.service.RateLimitEnforcer;
import com.runtime_crew.api_simulator.service.RequestLogService;
import com.runtime_crew.api_simulator.service.RequestLogger;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BarChartViewTest {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 9, 20, 22, 45, 6);

    @Test
    public void testAggregateAllowedAndBlockedPerService() {
        Map<String, int[]> serviceStats = new LinkedHashMap<>();

        BarChartView.aggregateServiceStats(List.of(
                record("MobileApp", "Facebook", false),
                record("MobileApp", "Facebook", true),
                record("MobileApp", "Facebook", false),
                record("MobileApp", "Instagram", true)
        ), serviceStats);

        assertEquals(2, serviceStats.size());
        assertArrayEquals(new int[]{2, 1}, serviceStats.get("Facebook"));
        assertArrayEquals(new int[]{0, 1}, serviceStats.get("Instagram"));
    }

    @Test
    public void testSingleServiceRecordsAreExcludedFromMultiServiceChart() {
        Map<String, int[]> serviceStats = new LinkedHashMap<>();

        BarChartView.aggregateServiceStats(List.of(
                record("WebApp", null, false),
                record("WebApp", "   ", true),
                record("WebApp", "-", false),
                record("WebApp", " Telegram ", false)
        ), serviceStats);

        assertFalse(serviceStats.containsKey("-"));
        assertEquals(1, serviceStats.size());
        assertArrayEquals(new int[]{1, 0}, serviceStats.get("Telegram"));
    }

    @Test
    public void testExistingCountersAreAccumulatedAndNullSafe() {
        Map<String, int[]> serviceStats = new LinkedHashMap<>();
        serviceStats.put("Messenger", new int[]{4, 2});

        BarChartView.aggregateServiceStats(new ArrayList<ActivityRecord>(), serviceStats);
        assertArrayEquals(new int[]{4, 2}, serviceStats.get("Messenger"));

        BarChartView.aggregateServiceStats(null, serviceStats);
        BarChartView.aggregateServiceStats(List.of(record("WebApp", "Messenger", true)), null);
        assertArrayEquals(new int[]{4, 2}, serviceStats.get("Messenger"));

        BarChartView.aggregateServiceStats(List.of(
                record("WebApp", "Messenger", true),
                record("WebApp", "Messenger", false)
        ), serviceStats);
        assertArrayEquals(new int[]{5, 3}, serviceStats.get("Messenger"));
    }

    @Test
    public void testTrackedMultiServiceRequestsProducePerServiceBars() {
        ClientActivityTracker activityTracker = new ClientActivityTracker();
        activityTracker.trackRequest(
                new ServiceRequest("MobileApp", "Facebook", RequestType.READ, BASE_TIME), false);
        activityTracker.trackRequest(
                new ServiceRequest("MobileApp", "Facebook", RequestType.WRITE, BASE_TIME.plusSeconds(1)), true);
        activityTracker.trackRequest(
                new ServiceRequest("MobileApp", RequestType.READ, BASE_TIME.plusSeconds(2)), false);

        var activity = activityTracker.getActivity("MobileApp");
        assertNotNull(activity);

        Map<String, int[]> serviceStats = new LinkedHashMap<>();
        BarChartView.aggregateServiceStats(activity.getRecords(), serviceStats);

        assertEquals(1, serviceStats.size());
        assertArrayEquals(new int[]{1, 1}, serviceStats.get("Facebook"));
    }

    @Test
    public void testMultiServiceSimulatorTrafficProducesPerServiceBars() throws IOException {
        Path tempLog = Files.createTempFile("multi_requests_test", ".csv");
        try {
            ClientActivityTracker activityTracker = new ClientActivityTracker();
            RequestLogger logger = new RequestLogger();
            RateLimitEnforcer enforcer = new RateLimitEnforcer(10, Duration.ofSeconds(10), Duration.ofSeconds(3), logger);
            RateLimitAnalyzer analyzer = new RateLimitAnalyzer(List.of(new FixedWindowPolicy(5, Duration.ofSeconds(10))));
            MultiServiceSimulator simulator = new MultiServiceSimulator(
                    logger, enforcer, activityTracker, analyzer, new RequestLogService(tempLog));

            simulator.sendRequest("MobileApp", List.of("Facebook", "Instagram"), RequestType.READ);

            var activity = activityTracker.getActivity("MobileApp");
            assertNotNull(activity);

            Map<String, int[]> serviceStats = new LinkedHashMap<>();
            BarChartView.aggregateServiceStats(activity.getRecords(), serviceStats);

            assertEquals(2, serviceStats.size());
            assertArrayEquals(new int[]{1, 0}, serviceStats.get("Facebook"));
            assertArrayEquals(new int[]{1, 0}, serviceStats.get("Instagram"));
        } finally {
            Files.deleteIfExists(tempLog);
        }
    }

    private static ActivityRecord record(String clientId, String service, boolean blocked) {
        return new ActivityRecord(clientId, service, BASE_TIME, RequestType.READ, blocked);
    }
}
