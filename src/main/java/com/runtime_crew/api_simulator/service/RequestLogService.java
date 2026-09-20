package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RequestLogService {

    private static final String HEADER = "requestId,clientId,serviceId,endpoint,method,timestamp,status,responseCode,responseTimeMs,rateLimit,remaining,windowSeconds,abuseDetected,abuseReason,severity,eventType,riskScore,reason";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final CsvFileHandler fileHandler;
    private final Set<String> knownIds = new HashSet<>();

    public RequestLogService() {
        this(Path.of("data", "requests.csv"));
    }

    public RequestLogService(Path csvFile) {
        this.fileHandler = new CsvFileHandler(csvFile);
        fileHandler.ensureFileExists(HEADER);
        for (String line : fileHandler.readLines()) {
            String[] parts = CsvParser.parseLine(line.trim());
            if (parts.length > 0 && !parts[0].isEmpty()) knownIds.add(parts[0]);
        }
    }

    public void append(String requestId, String clientId, RequestType type,
            boolean blocked, int rateLimit, int remaining, long windowSeconds,
            String eventType, int riskScore, String reason) {
        append(requestId, clientId, null, type, blocked, rateLimit, remaining, windowSeconds, eventType, riskScore, reason);
    }

    public void append(String requestId, String clientId, String serviceId, RequestType type,
            boolean blocked, int rateLimit, int remaining, long windowSeconds,
            String eventType, int riskScore, String reason) {
        if (knownIds.contains(requestId)) return;
        String line = CsvParser.buildCsvLine(requestId, clientId, serviceId, type,
                LocalDateTime.now().format(FMT), blocked,
                new Random().nextInt(136) + 15, rateLimit, remaining,
                windowSeconds, false, "", "NORMAL",
                eventType, riskScore, reason);
        fileHandler.appendLine(line);
        knownIds.add(requestId);
    }

    public List<CsvRequestLogEntry> loadAll() {
        List<CsvRequestLogEntry> entries = new ArrayList<>();
        for (String line : fileHandler.readLines()) {
            String[] p = CsvParser.parseLine(line.trim());
            if (p.length < 14) continue;
            try { entries.add(toEntry(p)); } catch (Exception ignored) {}
        }
        return entries;
    }

    private CsvRequestLogEntry toEntry(String[] p) {
        CsvRequestLogEntry e = new CsvRequestLogEntry();
        boolean hasServiceCol = p.length >= 18;

        if (hasServiceCol) {
            e.requestId = p[0];
            e.clientId = p[1];
            e.serviceId = p[2];
            e.endpoint = p[3];
            e.method = p[4];
            e.timestamp = LocalDateTime.parse(p[5], FMT);
            e.status = p[6];
            e.responseCode = Integer.parseInt(p[7]);
            e.responseTimeMs = Integer.parseInt(p[8]);
            e.rateLimit = Integer.parseInt(p[9]);
            e.remaining = Integer.parseInt(p[10]);
            e.windowSeconds = Long.parseLong(p[11]);
            e.abuseDetected = Boolean.parseBoolean(p[12]);
            e.abuseReason = p[13];
            e.severity = p[14];
            if (p.length > 15) e.eventType = p[15];
            if (p.length > 16) e.riskScore = Integer.parseInt(p[16]);
            if (p.length > 17) e.reason = p[17];
        } else {
            e.requestId = p[0];
            e.clientId = p[1];
            e.serviceId = "";
            e.endpoint = p[2];
            e.method = p[3];
            e.timestamp = LocalDateTime.parse(p[4], FMT);
            e.status = p[5];
            e.responseCode = Integer.parseInt(p[6]);
            e.responseTimeMs = Integer.parseInt(p[7]);
            e.rateLimit = Integer.parseInt(p[8]);
            e.remaining = Integer.parseInt(p[9]);
            e.windowSeconds = Long.parseLong(p[10]);
            e.abuseDetected = Boolean.parseBoolean(p[11]);
            e.abuseReason = p[12];
            e.severity = p[13];
            if (p.length > 14) e.eventType = p[14];
            if (p.length > 15) e.riskScore = Integer.parseInt(p[15]);
            if (p.length > 16) e.reason = p[16];
        }
        return e;
    }

    public void removeClient(String clientId) {
        List<String> kept = fileHandler.readLines().stream().skip(1)
                .filter(line -> {
                    String[] p = CsvParser.parseLine(line.trim());
                    boolean match = p.length > 1 && p[1].equals(clientId);
                    if (match) knownIds.remove(p[0]);
                    return !match;
                }).collect(Collectors.toList());
        fileHandler.rewriteFile(HEADER, kept);
    }

    public void moveClientLogs(String oldId, String newId) {
        List<String> updated = fileHandler.readLines().stream()
                .map(line -> {
                    String[] p = CsvParser.parseLine(line.trim());
                    if (p.length > 1 && p[1].equals(oldId)) {
                        return line.replaceFirst(
                                CsvParser.escape(oldId) + ",",
                                CsvParser.escape(newId) + ",");
                    }
                    return line;
                }).collect(Collectors.toList());
        fileHandler.rewriteFile(HEADER, updated.subList(1, updated.size()));
    }

    public void clearAll() { knownIds.clear(); fileHandler.clearFile(HEADER); }
    public int getEntryCount() { return knownIds.size(); }
}
