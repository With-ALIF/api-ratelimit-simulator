package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.model.ServiceRequest;
import com.runtime_crew.api_simulator.service.ClientActivityTracker.ClientActivity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

        String report = generator.generateAllClientsFullReport(activities, false);
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

    @Test
    public void testBuildServiceComparisonGroupsActivityByService() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 20, 22, 45, 6);
        Map<String, ClientActivity> activities = new LinkedHashMap<>();

        ClientActivity webApp = new ClientActivity("WebApp");
        webApp.recordActivity(new ServiceRequest("WebApp", "Facebook", RequestType.READ, time), false);
        webApp.recordActivity(new ServiceRequest("WebApp", "Facebook", RequestType.WRITE, time.plusSeconds(1)), true);
        webApp.recordActivity(new ServiceRequest("WebApp", RequestType.READ, time.plusSeconds(2)), false);
        activities.put("WebApp", webApp);

        ClientActivity mobileApp = new ClientActivity("MobileApp");
        mobileApp.recordActivity(new ServiceRequest("MobileApp", "Instagram", RequestType.DELETE, time.plusSeconds(3)), true);
        activities.put("MobileApp", mobileApp);

        Map<String, ClientActivity> perService = new EnhancedReportGenerator().buildServiceComparison(activities);

        assertEquals(2, perService.size());
        assertFalse(perService.containsKey("WebApp"));

        ClientActivity facebook = perService.get("Facebook");
        assertNotNull(facebook);
        assertEquals(2, facebook.getTotalRequests());
        assertEquals(1, facebook.getAllowedRequests());
        assertEquals(1, facebook.getBlockedRequests());
        assertEquals(50.0, facebook.getSuccessRate());

        ClientActivity instagram = perService.get("Instagram");
        assertNotNull(instagram);
        assertEquals(1, instagram.getTotalRequests());
        assertEquals(0, instagram.getAllowedRequests());
        assertEquals(1, instagram.getBlockedRequests());
        assertEquals("22:45:09", instagram.getLastActivityTime());
    }

    @Test
    public void testBuildServiceComparisonSkipsSingleServiceRecordsAndNulls() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 20, 22, 45, 6);
        Map<String, ClientActivity> activities = new LinkedHashMap<>();

        ClientActivity webApp = new ClientActivity("WebApp");
        webApp.recordActivity(new ServiceRequest("WebApp", RequestType.READ, time), false);
        webApp.recordActivity(new ServiceRequest("WebApp", "   ", RequestType.READ, time.plusSeconds(1)), true);
        webApp.recordActivity(new ServiceRequest("WebApp", "-", RequestType.READ, time.plusSeconds(2)), false);
        activities.put("WebApp", webApp);

        EnhancedReportGenerator generator = new EnhancedReportGenerator();
        assertTrue(generator.buildServiceComparison(activities).isEmpty());
        assertTrue(generator.buildServiceComparison(null).isEmpty());
    }

    @Test
    public void testQuickSummaryClientTableIsAligned() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 21, 11, 0, 0);
        Map<String, ClientActivity> activities = new LinkedHashMap<>();
        activities.put("PartnerAPI", activity("PartnerAPI", 17, 12, time));
        activities.put("SuspiciousBot", activity("SuspiciousBot", 17, 12, time));
        activities.put("MobileApp", activity("MobileApp", 21, 8, time));
        activities.put("WebApp", activity("WebApp", 21, 8, time));

        String report = new EnhancedReportGenerator()
                .generateAllClientsUsageReport(116, 76, 40, 65.5, activities, false);
        List<String> lines = List.of(report.split("\n"));

        String header = lines.get(assertAlignedTable(lines, "  CLIENT", 4));

        assertTrue(header.endsWith(String.format("%8s", "RATE")), "header RATE column: [" + header + "]");
        assertEquals(String.format("%8s", "58.6%"),
                lines.get(indexOfLine(lines, "  PartnerAPI")).substring(header.length() - 8));
        assertEquals(String.format("%8s", "65.5%"),
                lines.get(indexOfLine(lines, "  TOTAL")).substring(header.length() - 8));
    }

    @Test
    public void testQuickSummaryAlignsWithLongClientName() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 21, 11, 0, 0);
        Map<String, ClientActivity> activities = new LinkedHashMap<>();
        activities.put("VeryLongClientNameThatExceedsDefaultWidth",
                activity("VeryLongClientNameThatExceedsDefaultWidth", 2, 1, time));

        String report = new EnhancedReportGenerator()
                .generateAllClientsUsageReport(3, 2, 1, 66.7, activities, false);
        List<String> lines = List.of(report.split("\n"));

        assertAlignedTable(lines, "  CLIENT", 1);
    }

    @Test
    public void testQuickSummaryServiceBreakdownIsAligned() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 21, 11, 0, 0);
        Map<String, ClientActivity> activities = new LinkedHashMap<>();

        ClientActivity webApp = new ClientActivity("WebApp");
        webApp.recordActivity(new ServiceRequest("WebApp", "Facebook", RequestType.READ, time), false);
        webApp.recordActivity(new ServiceRequest("WebApp", "Instagram", RequestType.WRITE, time), true);
        activities.put("WebApp", webApp);

        String report = new EnhancedReportGenerator()
                .generateAllClientsUsageReport(2, 1, 1, 50.0, activities, true);
        List<String> lines = List.of(report.split("\n"));

        int serviceHeaderIdx = assertAlignedTable(lines, "  SERVICE", 2);
        String[] totalFields = lines.get(serviceHeaderIdx + 5).trim().split("\\s+");
        assertEquals("TOTAL 2 1 1", String.join(" ", totalFields));
    }

    private static ClientActivity activity(String id, int allowed, int blocked, LocalDateTime time) {
        ClientActivity a = new ClientActivity(id);
        for (int i = 0; i < allowed; i++) {
            a.recordActivity(new ServiceRequest(id, RequestType.READ, time.plusSeconds(i)), false);
        }
        for (int i = 0; i < blocked; i++) {
            a.recordActivity(new ServiceRequest(id, RequestType.WRITE, time.plusSeconds(100 + i)), true);
        }
        return a;
    }

    private static int indexOfLine(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) return i;
        }
        return -1;
    }

    /** Finds the real table header: the line matching the prefix that is followed by a full-width rule. */
    private static int indexOfTableHeader(List<String> lines, String headerPrefix) {
        for (int i = 0; i + 1 < lines.size(); i++) {
            String line = lines.get(i);
            String next = lines.get(i + 1);
            if (line.startsWith(headerPrefix)
                    && next.startsWith("  ")
                    && next.substring(2).matches("-+")
                    && next.length() == line.length()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Asserts a fixed-width ASCII table (header, rule, data rows, rule, TOTAL row):
     * every line must have exactly the same width so the columns stay aligned.
     * Returns the index of the header line.
     */
    private static int assertAlignedTable(List<String> lines, String headerPrefix, int dataRows) {
        int headerIdx = indexOfTableHeader(lines, headerPrefix);
        assertTrue(headerIdx >= 0, "table header not found: [" + headerPrefix + "]");

        String header = lines.get(headerIdx);
        String rule = "  " + "-".repeat(header.length() - 2);
        assertEquals(rule, lines.get(headerIdx + 1), "rule under header");

        for (int i = 0; i < dataRows; i++) {
            String row = lines.get(headerIdx + 2 + i);
            assertEquals(header.length(), row.length(), "row width mismatch: [" + row + "]");
        }

        assertEquals(rule, lines.get(headerIdx + 2 + dataRows), "rule above TOTAL");
        assertEquals(header.length(), lines.get(headerIdx + 3 + dataRows).length(), "TOTAL row width mismatch");
        return headerIdx;
    }
}
