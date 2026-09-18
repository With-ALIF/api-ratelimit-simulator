package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;

import java.time.LocalDateTime;

public class CsvRequestLogEntry {

    public String requestId;
    public String clientId;
    public String endpoint;
    public String method;
    public LocalDateTime timestamp;
    public String status;
    public int responseCode;
    public int responseTimeMs;
    public int rateLimit;
    public int remaining;
    public long windowSeconds;
    public boolean abuseDetected;
    public String abuseReason;
    public String severity;

    public boolean isBlocked() {
        return "BLOCKED".equals(status);
    }

    public RequestType getRequestType() {
        if (method == null) return RequestType.READ;
        return switch (method) {
            case "GET" -> RequestType.READ;
            case "POST" -> RequestType.WRITE;
            case "PUT" -> RequestType.UPDATE;
            case "DELETE" -> RequestType.DELETE;
            default -> RequestType.READ;
        };
    }
}
