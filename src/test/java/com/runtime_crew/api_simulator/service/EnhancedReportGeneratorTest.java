package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.model.ServiceRequest;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EnhancedReportGeneratorTest {

    @Test
    public void testGenerateAllClientsFullReportOutput() {
        EnhancedReportGenerator generator = new EnhancedReportGenerator();

        Map<String, ClientActivity> activities = new LinkedHashMap<>();
        LocalDateTime time = LocalDateTime.of(2026, 9, 5, 9, 11, 35);

        for (String client : new String[]{"CLIENT_A", "CLIENT_B", "CLIENT_C", "CLIENT_D"}) {
            ClientActivity act = new ClientActivity(client);
       
            for (int i = 0; i < 8; i++) {
                act.recordActivity(new ServiceRequest(client, RequestType.READ, time), false);
            }
      
            for (int i = 0; i < 72; i++) {
                act.recordActivity(new ServiceRequest(client, RequestType.READ, time), true);
            }
            activities.put(client, act);
        }

        String report = generator.generateAllClientsFullReport(activities);
        System.out.println("====== GENERATED REPORT TEST OUTPUT ======");
        System.out.println(report);
        System.out.println("==========================================");

        assertNotNull(report);
        assertTrue(report.contains("ALL CLIENTS - FULL REPORT"));
        assertTrue(report.contains("API RATE-LIMIT & ABUSE SIMULATOR"));
        assertTrue(report.contains("OVERALL"));
        assertTrue(report.contains("CLIENT PERFORMANCE"));
        assertTrue(report.contains("RATE-LIMIT STATUS"));
        assertTrue(report.contains("SUMMARY"));
        assertTrue(report.contains("END OF REPORT"));
    }
}
