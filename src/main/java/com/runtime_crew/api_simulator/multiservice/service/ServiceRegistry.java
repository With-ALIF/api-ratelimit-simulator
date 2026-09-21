package com.runtime_crew.api_simulator.multiservice.service;

import com.runtime_crew.api_simulator.multiservice.model.ApiService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ServiceRegistry {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path CSV_FILE = DATA_DIR.resolve("api_services.csv");
    private static final String CSV_HEADER = "id,name,isDefault,icon,enabledEventTypes";

    private final LinkedHashMap<String, ApiService> services = new LinkedHashMap<>();

    public ServiceRegistry() {
        ensureFileExists();
        loadExisting();
        registerDefaults();
    }

    private void registerDefaults() {
        registerIfAbsent(new ApiService("Facebook", "Facebook", true, "\uD83D\uDCD3",
                ApiService.defaultEventTypes()));
        registerIfAbsent(new ApiService("Messenger", "Messenger", true, "\uD83D\uDCE8",
                ApiService.defaultEventTypes()));
        registerIfAbsent(new ApiService("Telegram", "Telegram", true, "\u2708\uFE0F",
                ApiService.defaultEventTypes()));
        registerIfAbsent(new ApiService("Instagram", "Instagram", true, "\uD83D\uDCF7",
                ApiService.defaultEventTypes()));
    }

    private void registerIfAbsent(ApiService service) {
        if (!services.containsKey(service.getId())) {
            services.put(service.getId(), service);
        }
    }

    private void ensureFileExists() {
        try {
            Files.createDirectories(DATA_DIR);
            if (!Files.exists(CSV_FILE)) {
                Files.writeString(CSV_FILE, CSV_HEADER + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Failed to create api_services.csv: " + e.getMessage());
        }
    }

    private void loadExisting() {
        try {
            List<String> lines = Files.readAllLines(CSV_FILE, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 5) {
                    String id = unescape(parts[0]);
                    String name = unescape(parts[1]);
                    boolean isDefault = Boolean.parseBoolean(parts[2].trim());
                    String icon = unescape(parts[3]);
                    Set<String> eventTypes = ApiService.parseEventTypes(parts[4]);
                    services.put(id, new ApiService(id, name, isDefault, icon, eventTypes));
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load api_services.csv: " + e.getMessage());
        }
    }

    public boolean addService(String id, String name, String icon, Set<String> eventTypes) {
        if (id == null || id.trim().isEmpty()) return false;
        id = id.trim();
        if (services.containsKey(id)) return false;
        ApiService service = new ApiService(id, name != null ? name.trim() : id, false,
                icon != null && !icon.trim().isEmpty() ? icon.trim() : "\uD83D\uDDA5\uFE0F",
                eventTypes);
        services.put(id, service);
        rewriteCsv();
        return true;
    }

    public boolean removeService(String id) {
        if (id == null || !services.containsKey(id)) return false;
        ApiService service = services.get(id);
        if (service.isDefault()) return false;
        services.remove(id);
        rewriteCsv();
        return true;
    }

    public boolean isDefault(String id) {
        ApiService s = services.get(id);
        return s != null && s.isDefault();
    }

    public List<ApiService> getAll() {
        return new ArrayList<>(services.values());
    }

    public ApiService get(String id) {
        return services.get(id);
    }

    public Set<String> getServiceEventTypes(String id) {
        ApiService s = services.get(id);
        return s != null ? s.getEnabledEventTypes() : ApiService.defaultEventTypes();
    }

    private void rewriteCsv() {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append(System.lineSeparator());
        for (ApiService s : services.values()) {
            sb.append(escape(s.getId())).append(",")
              .append(escape(s.getName())).append(",")
              .append(s.isDefault()).append(",")
              .append(escape(s.getIcon())).append(",")
              .append(escape(s.toEventTypesString()))
              .append(System.lineSeparator());
        }
        try {
            Files.writeString(CSV_FILE, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to rewrite api_services.csv: " + e.getMessage());
        }
    }

    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String unescape(String value) {
        if (value == null) return "";
        value = value.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
            value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }
}
