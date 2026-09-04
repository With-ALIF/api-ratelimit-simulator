package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.RequestLog;
import com.async_alpha.api_simulator.model.ServiceRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RateLimitEnforcer {

    private final int maxRequests;
    private final Duration timeWindow;
    private final RequestLogger requestLogger;

    public RateLimitEnforcer(int maxRequests, Duration timeWindow, RequestLogger requestLogger) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.requestLogger = requestLogger;
    }

    public boolean shouldBlock(ServiceRequest request) {
        RequestLog log = requestLogger.getLog(request.getClientId());
        if (log == null || log.getRequests().isEmpty()) {
            return false;
        }

        LocalDateTime requestTime = request.getTimestamp();
        long recentCount = log.getRequests().stream()
            .filter(r -> {
                Duration diff = Duration.between(r.getTimestamp(), requestTime);
                return !diff.isNegative() && diff.compareTo(timeWindow) <= 0;
            })
            .count();

        return recentCount >= maxRequests;
    }

    public RequestResult processRequest(ServiceRequest request) {
        boolean blocked = shouldBlock(request);
        if (!blocked) {
            requestLogger.logRequest(request);
        }
        return new RequestResult(request, blocked, getRemainingQuota(request.getClientId()));
    }

    public int getRemainingQuota(String clientId) {
        RequestLog log = requestLogger.getLog(clientId);
        if (log == null || log.getRequests().isEmpty()) {
            return maxRequests;
        }

        LocalDateTime now = LocalDateTime.now();
        List<ServiceRequest> requests = log.getRequests();
        LocalDateTime latest = requests.get(requests.size() - 1).getTimestamp();
        LocalDateTime refTime = latest.isAfter(now) ? latest : now;

        long recentCount = requests.stream()
            .filter(r -> {
                Duration diff = Duration.between(r.getTimestamp(), refTime);
                return !diff.isNegative() && diff.compareTo(timeWindow) <= 0;
            })
            .count();

        return Math.max(0, maxRequests - (int) recentCount);
    }

    public Duration getTimeUntilReset(String clientId) {
        RequestLog log = requestLogger.getLog(clientId);
        if (log == null || log.getRequests().isEmpty()) {
            return Duration.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldestInWindow = now.minus(timeWindow);

        LocalDateTime oldestRequest = log.getRequests().stream()
            .map(ServiceRequest::getTimestamp)
            .filter(t -> t.isAfter(oldestInWindow))
            .min(LocalDateTime::compareTo)
            .orElse(now);

        LocalDateTime resetTime = oldestRequest.plus(timeWindow);
        if (resetTime.isBefore(now)) {
            return Duration.ZERO;
        }
        return Duration.between(now, resetTime);
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public Duration getTimeWindow() {
        return timeWindow;
    }

    public static class RequestResult {
        private final ServiceRequest request;
        private final boolean blocked;
        private final int remainingQuota;

        public RequestResult(ServiceRequest request, boolean blocked, int remainingQuota) {
            this.request = request;
            this.blocked = blocked;
            this.remainingQuota = remainingQuota;
        }

        public ServiceRequest getRequest() { return request; }

        public boolean isBlocked() { return blocked; }

        public boolean isAllowed() { return !blocked; }

        public int getRemainingQuota() { return remainingQuota; }

        public String getStatusMessage() {
            return blocked
                ? "REQUEST BLOCKED - Rate limit exceeded"
                : "REQUEST ALLOWED - Remaining quota: " + remainingQuota;
        }
    }
}