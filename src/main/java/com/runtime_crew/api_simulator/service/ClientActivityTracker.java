package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClientActivityTracker {

    private final Map<String, ClientActivity> activities = new HashMap<>();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    public void registerClient(Client client) {
        if (client != null) {
            activities.computeIfAbsent(client.getClientId(), id -> new ClientActivity(id, client.getName()));
        }
    }

    public void registerClient(String clientId, String name) {
        if (clientId != null && !clientId.trim().isEmpty()) {
            activities.computeIfAbsent(clientId.trim(), id -> new ClientActivity(id, name));
        }
    }

    public void trackRequest(ServiceRequest request, boolean wasBlocked) {
        activities
            .computeIfAbsent(request.getClientId(), ClientActivity::new)
            .recordActivity(request, wasBlocked);
    }

    public ClientActivity getActivity(String clientId) {
        return activities.get(clientId);
    }

    public Map<String, ClientActivity> getAllActivities() {
        return Collections.unmodifiableMap(activities);
    }

    public void clearClient(String clientId) {
        activities.remove(clientId);
    }

    public void clearAll() {
        activities.clear();
    }

    public static class ClientActivity {
        private final String clientId;
        private final String name;
        private final List<ActivityRecord> records = new ArrayList<>();
        private int totalRequests = 0;
        private int blockedRequests = 0;
        private int allowedRequests = 0;

        public ClientActivity(String clientId) {
            this(clientId, clientId);
        }

        public ClientActivity(String clientId, String name) {
            this.clientId = clientId;
            this.name = (name != null && !name.trim().isEmpty()) ? name.trim() : clientId;
        }

        public String getName() {
            return name;
        }

        public void recordActivity(ServiceRequest request, boolean blocked) {
            totalRequests++;
            if (blocked) {
                blockedRequests++;
            } else {
                allowedRequests++;
            }

            ActivityRecord record = new ActivityRecord(
                clientId,
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
            return Collections.unmodifiableList(records);
        }

        public String getLastActivityTime() {
            if (records.isEmpty()) return "N/A";
            return records.get(records.size() - 1).getTimestamp().format(formatter);
        }
    }

    public static class ActivityRecord {
        private final String clientId;
        private final LocalDateTime timestamp;
        private final RequestType requestType;
        private final boolean blocked;

        public ActivityRecord(String clientId, LocalDateTime timestamp, RequestType requestType, boolean blocked) {
            this.clientId = clientId;
            this.timestamp = timestamp;
            this.requestType = requestType;
            this.blocked = blocked;
        }

        public String getClientId() {
            return clientId;
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