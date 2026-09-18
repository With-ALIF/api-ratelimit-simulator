package com.runtime_crew.api_simulator.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CsvFileHandler {

    private final Path filePath;

    public CsvFileHandler(Path filePath) {
        this.filePath = filePath;
    }

    public void ensureFileExists(String header) {
        try {
            Files.createDirectories(filePath.getParent());
            if (!Files.exists(filePath)) {
                Files.writeString(filePath, header + System.lineSeparator(),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.err.println("Failed to create CSV: " + e.getMessage());
        }
    }

    public List<String> readLines() {
        try {
            return Files.readAllLines(filePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to read CSV: " + e.getMessage());
            return List.of();
        }
    }

    public void appendLine(String line) {
        try {
            Files.writeString(filePath, line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    public void rewriteFile(String header, List<String> keptLines) {
        try {
            List<String> all = new ArrayList<>();
            all.add(header);
            all.addAll(keptLines);
            Files.write(filePath, all, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to rewrite CSV: " + e.getMessage());
        }
    }

    public void clearFile(String header) {
        try {
            Files.writeString(filePath, header + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Failed to clear CSV: " + e.getMessage());
        }
    }
}
