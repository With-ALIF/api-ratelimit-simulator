package com.runtime_crew.api_simulator.multiservice.service;

import com.runtime_crew.api_simulator.model.*;
import com.runtime_crew.api_simulator.service.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class MultiServiceSimulator {

    private final RequestLogger logger;
    private final RateLimitEnforcer enforcer;
    private final ClientActivityTracker activityTracker;
    private final RateLimitAnalyzer analyzer;
    private final RequestLogService requestLogService;
    private final BurstTrafficGenerator burstGenerator = new BurstTrafficGenerator();
    private final Random random = new Random();

    public MultiServiceSimulator(RequestLogger logger, RateLimitEnforcer enforcer,
                                  ClientActivityTracker activityTracker, RateLimitAnalyzer analyzer,
                                  RequestLogService requestLogService) {
        this.logger = logger;
        this.enforcer = enforcer;
        this.activityTracker = activityTracker;
        this.analyzer = analyzer;
        this.requestLogService = requestLogService;
    }

    private String toClientId(String serviceId) {
        return "svc_" + serviceId;
    }

    public Map<String, ServiceResult> sendRequest(List<String> serviceIds, RequestType type) {
        return sendRequest("WebApp", serviceIds, type);
    }

    public Map<String, ServiceResult> sendRequest(String clientId, List<String> serviceIds, RequestType type) {
        String actualClient = (clientId != null && !clientId.trim().isEmpty() && !clientId.startsWith("svc_"))
                ? clientId.trim() : "WebApp";
        Map<String, ServiceResult> results = new LinkedHashMap<>();
        if (type == null) {
            RequestType[] types = RequestType.values();
            type = types[random.nextInt(types.length)];
        }
        for (String serviceId : serviceIds) {
            String serviceKey = toClientId(serviceId);
            LocalDateTime now = LocalDateTime.now();
            ServiceRequest enforceReq = new ServiceRequest(serviceKey, serviceId, type, now);
            RateLimitEnforcer.RequestResult result = enforcer.processRequest(enforceReq);

            ServiceRequest clientReq = new ServiceRequest(actualClient, serviceId, type, now);
            activityTracker.trackRequest(clientReq, result.isBlocked());

            RequestLog log = logger.getLog(serviceKey);
            AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(serviceKey);
            EventType eventType = determineEventType(result, abuse);
            int riskScore = abuse.getRiskScore();
            String reason = abuse.hasViolations() ? abuse.getSummary()
                    : (result.isBlocked() ? "RATE_LIMIT_EXCEEDED" : "OK");

            requestLogService.append(
                    UUID.randomUUID().toString(), actualClient, serviceId, clientReq.getRequestType(),
                    result.isBlocked(), enforcer.getMaxRequests(), result.getRemainingQuota(),
                    enforcer.getTimeWindow().getSeconds(),
                    eventType.name(), riskScore, reason
            );

            results.put(serviceId, new ServiceResult(serviceId, actualClient, clientReq, result, eventType, riskScore, reason));
        }
        return results;
    }

    public Map<String, BurstResult> simulateBurst(List<String> serviceIds, int count, Duration duration) {
        return simulateBurst("WebApp", serviceIds, count, duration);
    }

    public Map<String, BurstResult> simulateBurst(String clientId, List<String> serviceIds, int count, Duration duration) {
        String actualClient = (clientId != null && !clientId.trim().isEmpty() && !clientId.startsWith("svc_"))
                ? clientId.trim() : "WebApp";
        Map<String, BurstResult> results = new LinkedHashMap<>();
        for (String serviceId : serviceIds) {
            String serviceKey = toClientId(serviceId);
            List<ServiceRequest> burst = burstGenerator.generateBurst(serviceKey, count, duration);
            int allowed = 0, blocked = 0;
            for (ServiceRequest req : burst) {
                RateLimitEnforcer.RequestResult res = enforcer.processRequest(req);
                ServiceRequest clientReq = new ServiceRequest(actualClient, serviceId, req.getRequestType(), req.getTimestamp());
                activityTracker.trackRequest(clientReq, res.isBlocked());

                RequestLog log = logger.getLog(serviceKey);
                AbuseReport abuse = log != null ? analyzer.analyze(log) : new AbuseReport(serviceKey);
                EventType eventType = determineEventType(res, abuse);
                int riskScore = abuse.getRiskScore();
                String reason = abuse.hasViolations() ? abuse.getSummary()
                        : (res.isBlocked() ? "RATE_LIMIT_EXCEEDED" : "OK");

                requestLogService.append(
                        UUID.randomUUID().toString(), actualClient, serviceId, clientReq.getRequestType(),
                        res.isBlocked(), enforcer.getMaxRequests(), res.getRemainingQuota(),
                        enforcer.getTimeWindow().getSeconds(),
                        eventType.name(), riskScore, reason
                );

                if (res.isBlocked()) blocked++; else allowed++;
            }
            results.put(serviceId, new BurstResult(serviceId, count, allowed, blocked));
        }
        return results;
    }

    private EventType determineEventType(RateLimitEnforcer.RequestResult result, AbuseReport abuse) {
        if (result.isBlocked()) {
            return abuse.hasViolations() ? EventType.ABUSE_DETECTED : EventType.RATE_LIMITED;
        }
        return abuse.hasViolations() ? EventType.ABUSE_DETECTED : EventType.ALLOWED;
    }

    public int getRemainingQuota(String serviceId) {
        return enforcer.getRemainingQuota(toClientId(serviceId));
    }

    public int getMaxRequests() {
        return enforcer.getMaxRequests();
    }

    public static class ServiceResult {
        public final String serviceId;
        public final String clientId;
        public final ServiceRequest request;
        public final RateLimitEnforcer.RequestResult enforcerResult;
        public final EventType eventType;
        public final int riskScore;
        public final String reason;

        public ServiceResult(String serviceId, String clientId, ServiceRequest request,
                             RateLimitEnforcer.RequestResult enforcerResult,
                             EventType eventType, int riskScore, String reason) {
            this.serviceId = serviceId;
            this.clientId = clientId;
            this.request = request;
            this.enforcerResult = enforcerResult;
            this.eventType = eventType;
            this.riskScore = riskScore;
            this.reason = reason;
        }

        public boolean isBlocked() { return enforcerResult.isBlocked(); }
        public int getRemainingQuota() { return enforcerResult.getRemainingQuota(); }
    }

    public static class BurstResult {
        public final String serviceId;
        public final int total;
        public final int allowed;
        public final int blocked;

        public BurstResult(String serviceId, int total, int allowed, int blocked) {
            this.serviceId = serviceId;
            this.total = total;
            this.allowed = allowed;
            this.blocked = blocked;
        }
    }
}
