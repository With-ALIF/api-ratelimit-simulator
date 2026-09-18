package com.runtime_crew.api_simulator.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClientRegistryService {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path CSV_FILE = DATA_DIR.resolve("clients.csv");
    private static final String CSV_HEADER = "clientId,name";

    private final Set<String> knownClients = new LinkedHashSet<>();

    public ClientRegistryService() {
        ensureFileExists();
        loadExisting();
        registerDefaults();
    }

    public boolean isDefaultClient(String clientId) {
        return "MobileApp".equals(clientId) || "WebApp".equals(clientId)
                || "PartnerAPI".equals(clientId) || "SuspiciousBot".equals(clientId);
    }

    private void registerDefaults() {
        register("MobileApp", "MobileApp");
        register("WebApp", "WebApp");
        register("PartnerAPI", "PartnerAPI");
        register("SuspiciousBot", "SuspiciousBot");
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(DATA_DIR);
            if (!Files.exists(CSV_FILE)) {
                Files.writeString(CSV_FILE, CSV_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Failed to create clients.csv: " + e.getMessage());
        }
    }

    private void loadExisting() {
        try {
            List<String> lines = Files.readAllLines(CSV_FILE, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 2);
                if (parts.length > 0 && !parts[0].isEmpty()) {
                    knownClients.add(parts[0].trim());
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load clients.csv: " + e.getMessage());
        }
    }

    public void register(String clientId, String name) {
        if (clientId == null || clientId.trim().isEmpty()) return;
        clientId = clientId.trim();
        if (knownClients.contains(clientId)) return;

        String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : clientId;
        String line = escape(clientId) + "," + escape(safeName);
        try {
            Files.writeString(CSV_FILE, line + System.lineSeparator(),
                    StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            knownClients.add(clientId);
        } catch (IOException e) {
            System.err.println("Failed to write client to CSV: " + e.getMessage());
        }
    }

    public List<String> loadAll() {
        return new ArrayList<>(knownClients);
    }

    public void removeClient(String clientId) {
        if (clientId == null || !knownClients.contains(clientId)) return;
        knownClients.remove(clientId);
        rewriteCsv();
    }

    public void renameClient(String oldId, String newId) {
        if (oldId == null || newId == null) return;
        if (!knownClients.contains(oldId) || knownClients.contains(newId)) return;
        knownClients.remove(oldId);
        knownClients.add(newId.trim());
        rewriteCsv();
    }

    private void rewriteCsv() {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append(System.lineSeparator());
        for (String cid : knownClients) {
            sb.append(escape(cid)).append(",").append(escape(cid)).append(System.lineSeparator());
        }
        try {
            Files.writeString(CSV_FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to rewrite clients.csv: " + e.getMessage());
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
