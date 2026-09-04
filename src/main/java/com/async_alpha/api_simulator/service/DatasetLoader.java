package com.async_alpha.api_simulator.service;

import com.async_alpha.api_simulator.model.RequestType;
import com.async_alpha.api_simulator.model.ServiceRequest;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DatasetLoader {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static class LoadResult {
        private final List<ServiceRequest> requests = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public List<ServiceRequest> getRequests() { return requests; }

        public List<String> getErrors() { return errors; }

        public int getSuccessCount() { return requests.size(); }

        public int getErrorCount() { return errors.size(); }
    }

    public LoadResult loadFromFile(File file) throws IOException {
        LoadResult result = new LoadResult();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            parseLines(reader, result);
        }
        return result;
    }

    public LoadResult loadFromText(String content) throws IOException {
        LoadResult result = new LoadResult();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            parseLines(reader, result);
        }
        return result;
    }

    private void parseLines(BufferedReader reader, LoadResult result) throws IOException {
        String line;
        int lineNumber = 0;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;
            try {
                ServiceRequest req = parseLine(line);
                if (req != null) result.requests.add(req);
            } catch (Exception e) {
                result.errors.add("Line " + lineNumber + ": " + e.getMessage() + " -> [" + line + "]");
            }
        }
    }

    public ServiceRequest parseLine(String line) {
        String[] tokens = line.contains(",") ? line.split(",")
            : (line.contains(";") ? line.split(";") : line.split("\\s+"));

        if (tokens.length < 3) {
            throw new IllegalArgumentException("Expected: <timestamp>, <clientId>, <requestType>");
        }

        String timeStr = tokens[0].trim();
        String clientId = tokens[1].trim();
        String typeStr = tokens[2].trim();

        if (timeStr.equalsIgnoreCase("timestamp") || clientId.equalsIgnoreCase("clientid")) return null;
        if (clientId.isEmpty()) throw new IllegalArgumentException("Client ID cannot be empty");

        LocalDateTime timestamp = parseTimestamp(timeStr);
        RequestType requestType = RequestType.valueOf(typeStr.toUpperCase());
        return new ServiceRequest(clientId, requestType, timestamp);
    }

    private LocalDateTime parseTimestamp(String timeStr) {
        try { return LocalDateTime.parse(timeStr); } catch (Exception ignored) {}
        try { return LocalDateTime.parse(timeStr, DATE_TIME_FMT); } catch (Exception ignored) {}
        try { return LocalDateTime.of(LocalDate.now(), LocalTime.parse(timeStr, TIME_FMT)); } catch (Exception ignored) {}
        try { return LocalDateTime.of(LocalDate.now(), LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"))); } catch (Exception ignored) {}
        throw new IllegalArgumentException("Unsupported timestamp format: '" + timeStr + "'");
    }
}
