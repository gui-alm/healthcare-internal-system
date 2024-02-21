package me.a06.meditrack.securedocument.Exceptions;

public class ClientNotFoundException extends Exception {
    private String clientName;

    public ClientNotFoundException(String client) {
        super("Could not find client " + client + " in metadata");
        clientName = client;
    }

    public String getClientName() {
        return clientName;
    }
}
