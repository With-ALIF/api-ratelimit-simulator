package com.async_alpha.api_simulator.model;

public class Client {

    private final String clientId;
    private final String name;

    public Client(String clientId, String name) {
        this.clientId = clientId;
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public String getName() {
        return name;
    }
}