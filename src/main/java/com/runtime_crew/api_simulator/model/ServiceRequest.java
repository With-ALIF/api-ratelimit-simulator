package com.runtime_crew.api_simulator.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ServiceRequest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String clientId;
    private final RequestType requestType;
    private final LocalDateTime timestamp;

    public ServiceRequest(String clientId, RequestType requestType, LocalDateTime timestamp) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("Client ID cannot be null or empty");
        }
        if (requestType == null) {
            throw new IllegalArgumentException("Request type cannot be null");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        this.clientId = clientId.trim();
        this.requestType = requestType;
        this.timestamp = timestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        return timestamp.format(TIME_FORMAT);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRequest that = (ServiceRequest) o;
        return Objects.equals(clientId, that.clientId) &&
                requestType == that.requestType &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId, requestType, timestamp);
    }

    @Override
    public String toString() {
        return String.format("ServiceRequest[%s, %s, %s]", clientId, requestType, timestamp.format(TIME_FORMAT));
    }
}