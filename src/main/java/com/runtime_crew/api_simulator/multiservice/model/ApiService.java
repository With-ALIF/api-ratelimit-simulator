package com.runtime_crew.api_simulator.multiservice.model;

import java.util.*;

public class ApiService {

    private final String id;
    private final String name;
    private final boolean isDefault;
    private final String icon;
    private final Set<String> enabledEventTypes;

    public ApiService(String id, String name, boolean isDefault, String icon, Set<String> enabledEventTypes) {
        this.id = id;
        this.name = name;
        this.isDefault = isDefault;
        this.icon = icon;
        this.enabledEventTypes = enabledEventTypes != null
                ? new LinkedHashSet<>(enabledEventTypes)
                : defaultEventTypes();
    }

    public ApiService(String id, String name, boolean isDefault, String icon) {
        this(id, name, isDefault, icon, defaultEventTypes());
    }

    public static Set<String> defaultEventTypes() {
        return new LinkedHashSet<>(Arrays.asList(
                "REQUEST", "ALLOWED", "RATE_LIMITED", "ABUSE_DETECTED", "BLOCKED"
        ));
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isDefault() { return isDefault; }
    public String getIcon() { return icon; }
    public Set<String> getEnabledEventTypes() { return Collections.unmodifiableSet(enabledEventTypes); }

    public boolean isEventTypeEnabled(String eventType) {
        return enabledEventTypes.contains(eventType);
    }

    public String toEventTypesString() {
        return String.join(";", enabledEventTypes);
    }

    public static Set<String> parseEventTypes(String s) {
        Set<String> types = new LinkedHashSet<>();
        if (s == null || s.trim().isEmpty()) return defaultEventTypes();
        for (String t : s.split(";")) {
            String trimmed = t.trim();
            if (!trimmed.isEmpty()) types.add(trimmed);
        }
        return types.isEmpty() ? defaultEventTypes() : types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiService that = (ApiService) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return icon + " " + name;
    }
}
