package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RequestLogService {

    private static final Path CSV_FILE = Path.of("data", "requests.csv");
    private static final String HEADER = "requestId,clientId,endpoint,method,timestamp,status,responseCode,responseTimeMs,rateLimit,remaining,windowSeconds,abuseDetected,abuseReason,severity";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final CsvFileHandler fileHandler = new CsvFileHandler(CSV_FILE);
    private final Set<String> knownIds = new HashSet<>();

    public RequestLogService() {
        fileHandler.ensureFileExists(HEADER);
        for (String line : fileHandler.readLines()) {
            String[] parts = CsvParser.parseLine(line.trim());
            if (parts.length > 0 && !parts[0].isEmpty()) knownIds.add(parts[0]);
        }
    }

    public void append(String requestId, String clientId, RequestType type,
            boolean blocked, int rateLimit, int remaining, long windowSeconds,
            boolean abuseDetected, String abuseReason, String severity) {
        if (knownIds.contains(requestId)) return;
        String line = CsvParser.buildCsvLine(requestId, clientId, type,
                LocalDateTime.now().format(FMT), blocked,
                new Random().nextInt(136) + 15, rateLimit, remaining,
                windowSeconds, abuseDetected, abuseReason, severity);
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
        e.requestId = p[0]; e.clientId = p[1]; e.endpoint = p[2];
        e.method = p[3]; e.status = p[5];
        e.timestamp = LocalDateTime.parse(p[4], FMT);
        e.responseCode = Integer.parseInt(p[6]);
        e.responseTimeMs = Integer.parseInt(p[7]);
        e.rateLimit = Integer.parseInt(p[8]);
        e.remaining = Integer.parseInt(p[9]);
        e.windowSeconds = Long.parseLong(p[10]);
        e.abuseDetected = Boolean.parseBoolean(p[11]);
        e.abuseReason = p[12]; e.severity = p[13];
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
