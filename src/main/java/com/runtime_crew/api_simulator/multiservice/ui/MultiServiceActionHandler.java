package com.runtime_crew.api_simulator.multiservice.ui;

import com.runtime_crew.api_simulator.model.RequestType;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator.BurstResult;
import com.runtime_crew.api_simulator.multiservice.service.MultiServiceSimulator.ServiceResult;
import com.runtime_crew.api_simulator.ui.dashboard.LogPanel;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MultiServiceActionHandler {

    private final MultiServiceSimulator simulator;
    private final Random random = new Random();
    private int burstCount = 20;
    private int burstDuration = 10;

    public MultiServiceActionHandler(MultiServiceSimulator simulator) {
        this.simulator = simulator;
    }

    public void handleSendRequest(List<String> serviceIds, RequestType type, LogPanel logPanel, Runnable onUpdated) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            logPanel.append("[MultiService] No services selected!\n");
            return;
        }

        if (type == null) {
            RequestType[] types = RequestType.values();
            type = types[random.nextInt(types.length)];
        }

        logPanel.append(String.format("\n\u2709\uFE0F Sending %s request to %d service(s)...\n", type, serviceIds.size()));

        Map<String, ServiceResult> results = simulator.sendRequest(serviceIds, type);
        int allowed = 0, blocked = 0;

        for (Map.Entry<String, ServiceResult> entry : results.entrySet()) {
            ServiceResult r = entry.getValue();
            String icon = r.isBlocked() ? "\u26D4" : "\u2705";
            String time = r.request.getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String eventTypeLabel = r.eventType != null ? r.eventType.getDisplayName() : "UNKNOWN";

            String line = String.format("%s [%s] %s | %s | Quota: %d/%d | Risk: %d | %s\n",
                    icon, time, entry.getKey(), eventTypeLabel,
                    r.getRemainingQuota(), simulator.getMaxRequests(),
                    r.riskScore, r.reason);
            logPanel.append(line);

            if (r.isBlocked()) blocked++; else allowed++;
        }

        logPanel.append(String.format("\u2705 Sent! Allowed: %d | Blocked: %d\n\n", allowed, blocked));
        if (onUpdated != null) onUpdated.run();
    }

    public void handleSimulateBurst(List<String> serviceIds, LogPanel logPanel, Runnable onUpdated) {
        if (serviceIds == null || serviceIds.isEmpty()) {
            logPanel.append("[MultiService] No services selected!\n");
            return;
        }

        logPanel.append(String.format("\n\u26A1 Burst simulation: %d requests over %ds for %d service(s)...\n",
                burstCount, burstDuration, serviceIds.size()));

        Map<String, BurstResult> results = simulator.simulateBurst(serviceIds, burstCount, Duration.ofSeconds(burstDuration));
        int totalAllowed = 0, totalBlocked = 0;

        for (Map.Entry<String, BurstResult> entry : results.entrySet()) {
            BurstResult r = entry.getValue();
            logPanel.append(String.format("  %s - Allowed: %d | Blocked: %d\n", r.serviceId, r.allowed, r.blocked));
            totalAllowed += r.allowed;
            totalBlocked += r.blocked;
        }

        int total = burstCount * serviceIds.size();
        logPanel.append(String.format("\u26A1 Burst Done! Total: %d | Allowed: %d | Blocked: %d\n\n",
                total, totalAllowed, totalBlocked));
        if (onUpdated != null) onUpdated.run();
    }

    public void updateBurstConfig(int burstCount, int burstDuration) {
        this.burstCount = burstCount;
        this.burstDuration = burstDuration;
    }

    public int getBurstCount() { return burstCount; }
    public int getBurstDuration() { return burstDuration; }
}
