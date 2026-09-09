package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestLog;
import com.runtime_crew.api_simulator.model.ServiceRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RateLimitEnforcer {

    private final int maxRequests;
    private final Duration timeWindow;
    private final Duration blockDuration;
    private final RequestLogger requestLogger;
    private final Map<String, LocalDateTime> blockStartTimes = new HashMap<>();

    public RateLimitEnforcer(int maxRequests, Duration timeWindow, RequestLogger requestLogger) {
        this(maxRequests, timeWindow, Duration.ofSeconds(3), requestLogger);
    }

    public RateLimitEnforcer(int maxRequests, Duration timeWindow, Duration blockDuration, RequestLogger requestLogger) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
        this.blockDuration = blockDuration;
        this.requestLogger = requestLogger;
    }

    public boolean shouldBlock(ServiceRequest request) {
        String clientId = request.getClientId();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime blockStart = blockStartTimes.get(clientId);
        if (blockStart != null) {
            if (Duration.between(blockStart, now).compareTo(blockDuration) < 0) {
                return true;
            }
            blockStartTimes.remove(clientId);
        }

        RequestLog log = requestLogger.getLog(clientId);
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

        if (recentCount >= maxRequests) {
            blockStartTimes.put(clientId, now);
            return true;
        }

        return false;
    }

    public RequestResult processRequest(ServiceRequest request) {
        boolean blocked = shouldBlock(request);
        if (!blocked) {
            requestLogger.logRequest(request);
        }
        return new RequestResult(request, blocked, getRemainingQuota(request.getClientId()));
    }

    public int getRemainingQuota(String clientId) {
        LocalDateTime blockStart = blockStartTimes.get(clientId);
        if (blockStart != null) {
            LocalDateTime now = LocalDateTime.now();
            if (Duration.between(blockStart, now).compareTo(blockDuration) < 0) {
                return 0;
            }
            blockStartTimes.remove(clientId);
            requestLogger.clearClient(clientId);
            return maxRequests;
        }

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

        LocalDateTime blockStart = blockStartTimes.get(clientId);
        if (blockStart != null) {
            Duration elapsed = Duration.between(blockStart, LocalDateTime.now());
            Duration remaining = blockDuration.minus(elapsed);
            if (!remaining.isNegative()) return remaining;
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

    public Duration getBlockDuration() {
        return blockDuration;
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