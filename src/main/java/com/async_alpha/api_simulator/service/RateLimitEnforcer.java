package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.*;
import com.async_alpha.api_simulator.policy.RatePolicy;

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

    /**
     * Check if request should be allowed or blocked based on rate limit
     * @return true if request should be BLOCKED, false if ALLOWED
     */
    public boolean shouldBlock(ServiceRequest request) {
        RequestLog log = requestLogger.getLog(request.getClientId());
        
        if (log == null || log.getRequests().isEmpty()) {
            return false; // No previous requests, allow it
        }

        LocalDateTime now = request.getTimestamp();
        
        // Count requests within the time window
        long recentRequestCount = log.getRequests().stream()
            .filter(r -> Duration.between(r.getTimestamp(), now).compareTo(timeWindow) <= 0)
            .count();

        // Block if limit exceeded
        return recentRequestCount >= maxRequests;
    }

    /**
     * Process request with rate limiting
     * @return RequestResult containing whether it was allowed/blocked
     */
    public RequestResult processRequest(ServiceRequest request) {
        boolean blocked = shouldBlock(request);
        
        if (!blocked) {
            // Only log if not blocked
            requestLogger.logRequest(request);
        }

        return new RequestResult(request, blocked, getRemainingQuota(request.getClientId()));
    }

    /**
     * Get remaining quota for a client
     */
    public int getRemainingQuota(String clientId) {
        RequestLog log = requestLogger.getLog(clientId);
        
        if (log == null || log.getRequests().isEmpty()) {
            return maxRequests;
        }

        LocalDateTime now = LocalDateTime.now();
        
        long recentCount = log.getRequests().stream()
            .filter(r -> Duration.between(r.getTimestamp(), now).compareTo(timeWindow) <= 0)
            .count();

        return Math.max(0, maxRequests - (int)recentCount);
    }

    /**
     * Get time until quota resets for a client
     */
    public Duration getTimeUntilReset(String clientId) {
        RequestLog log = requestLogger.getLog(clientId);
        
        if (log == null || log.getRequests().isEmpty()) {
            return Duration.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oldestInWindow = now.minus(timeWindow);
        
        // Find oldest request in current window
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

    /**
     * Result of processing a request
     */
    public static class RequestResult {
        private final ServiceRequest request;
        private final boolean blocked;
        private final int remainingQuota;

        public RequestResult(ServiceRequest request, boolean blocked, int remainingQuota) {
            this.request = request;
            this.blocked = blocked;
            this.remainingQuota = remainingQuota;
        }

        public ServiceRequest getRequest() {
            return request;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public boolean isAllowed() {
            return !blocked;
        }

        public int getRemainingQuota() {
            return remainingQuota;
        }

        public String getStatusMessage() {
            if (blocked) {
                return "⛔ REQUEST BLOCKED - Rate limit exceeded";
            } else {
                return "✅ REQUEST ALLOWED - Remaining quota: " + remainingQuota;
            }
        }
    }
}