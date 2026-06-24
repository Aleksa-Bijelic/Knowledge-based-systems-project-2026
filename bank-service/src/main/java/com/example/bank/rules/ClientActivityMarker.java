package com.example.bank.rules;

public class ClientActivityMarker {

    private final Long clientId;

    public ClientActivityMarker(Long clientId) {
        this.clientId = clientId;
    }

    public Long getClientId() {
        return clientId;
    }
}
