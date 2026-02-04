package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientActivityTracker {

    private final Map<String, ClientActivity> activities = new HashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void trackRequest(ServiceRequest request, boolean wasBlocked) {
        activities
            .computeIfAbsent(request.getClientId(), ClientActivity::new)
            .recordActivity(request, wasBlocked);
    }

    public ClientActivity getActivity(String clientId) {
        return activities.get(clientId);
    }

    public Map<String, ClientActivity> getAllActivities() {
        return activities;
    }

    public static class ClientActivity {
        private final String clientId;
        private final List<ActivityRecord> records = new ArrayList<>();
        private int totalRequests = 0;
        private int blockedRequests = 0;
        private int allowedRequests = 0;

        public ClientActivity(String clientId) {
            this.clientId = clientId;
        }

        public void recordActivity(ServiceRequest request, boolean blocked) {
            totalRequests++;
            if (blocked) {
                blockedRequests++;
            } else {
                allowedRequests++;
            }

            ActivityRecord record = new ActivityRecord(
                request.getTimestamp(),
                request.getRequestType(),
                blocked
            );
            records.add(record);
        }

        public String getClientId() {
            return clientId;
        }

        public int getTotalRequests() {
            return totalRequests;
        }

        public int getBlockedRequests() {
            return blockedRequests;
        }

        public int getAllowedRequests() {
            return allowedRequests;
        }

        public double getSuccessRate() {
            if (totalRequests == 0) return 100.0;
            return (allowedRequests * 100.0) / totalRequests;
        }

        public List<ActivityRecord> getRecords() {
            return records;
        }

        public String getLastActivityTime() {
            if (records.isEmpty()) return "N/A";
            return records.get(records.size() - 1).getTimestamp().format(formatter);
        }
    }

    public static class ActivityRecord {
        private final LocalDateTime timestamp;
        private final RequestType requestType;
        private final boolean blocked;

        public ActivityRecord(LocalDateTime timestamp, RequestType requestType, boolean blocked) {
            this.timestamp = timestamp;
            this.requestType = requestType;
            this.blocked = blocked;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public RequestType getRequestType() {
            return requestType;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getStatus() {
            return blocked ? "BLOCKED" : "ALLOWED";
        }

        public String getFormattedTime() {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        }
    }
}