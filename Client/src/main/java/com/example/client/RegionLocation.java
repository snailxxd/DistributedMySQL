package com.example.client;

public class RegionLocation {

    private final String host;
    private final int port;

    public RegionLocation(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String toHostPort() {
        return host + ":" + port;
    }

    public static RegionLocation parse(String hostPort) {
        if (hostPort == null) {
            return null;
        }
        String[] parts = hostPort.trim().split(":");
        if (parts.length != 2) {
            return null;
        }
        String host = parts[0];
        int port;
        try {
            port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        return new RegionLocation(host, port);
    }

    @Override
    public String toString() {
        return toHostPort();
    }
}

