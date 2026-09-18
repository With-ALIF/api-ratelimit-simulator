package com.runtime_crew.api_simulator.service;

import com.runtime_crew.api_simulator.model.RequestType;

import java.util.ArrayList;
import java.util.List;

public class CsvParser {

    public static String buildCsvLine(String requestId, String clientId,
            RequestType type, String timestamp, boolean blocked,
            int responseTimeMs, int rateLimit, int remaining,
            long windowSeconds, boolean abuseDetected,
            String abuseReason, String severity) {
        String status = blocked ? "BLOCKED" : "ALLOWED";
        return String.join(",",
                escape(requestId), escape(clientId),
                escape(mapEndpoint(type)), escape(mapMethod(type)),
                escape(timestamp), escape(status),
                blocked ? "429" : "200", String.valueOf(responseTimeMs),
                String.valueOf(rateLimit), String.valueOf(remaining),
                String.valueOf(windowSeconds), String.valueOf(abuseDetected),
                escape(abuseReason != null ? abuseReason : ""),
                escape(severity != null ? severity : "NORMAL"));
    }

    public static String[] parseLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); i++;
                    } else { inQuotes = false; }
                } else { current.append(c); }
            } else {
                if (c == '"') { inQuotes = true; }
                else if (c == ',') { result.add(current.toString()); current.setLength(0); }
                else { current.append(c); }
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    public static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public static String mapEndpoint(RequestType type) {
        return switch (type) {
            case READ -> "/api/read"; case WRITE -> "/api/write";
            case UPDATE -> "/api/update"; case DELETE -> "/api/delete";
        };
    }

    public static String mapMethod(RequestType type) {
        return switch (type) {
            case READ -> "GET"; case WRITE -> "POST";
            case UPDATE -> "PUT"; case DELETE -> "DELETE";
        };
    }
}
