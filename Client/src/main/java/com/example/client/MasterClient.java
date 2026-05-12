package com.example.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class MasterClient {

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public MasterClient(String host, int port) {
        this(host, port, 3000);
    }

    public MasterClient(String host, int port, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 发送一行 SQL 到 Master，返回原始响应行
     */
    public String sendSql(String sql) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.write(sql + "\n");
            writer.flush();

            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Empty response from Master");
            }
            return line.trim();
        }
    }

    /**
     * 协议：发送 "LOCATE_TABLE <table>"，返回 "REGION host:port" 或 "ERR message"
     */
    public RegionLocation locateTable(String table) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.write("LOCATE_TABLE " + table + "\n");
            writer.flush();

            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Empty response from Master");
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2 && "REGION".equalsIgnoreCase(parts[0])) {
                RegionLocation location = RegionLocation.parse(parts[1]);
                if (location == null) {
                    throw new IOException("Invalid REGION response: " + line);
                }
                return location;
            }

            if (parts.length >= 2 && "ERR".equalsIgnoreCase(parts[0])) {
                throw new IOException(line.substring(4));
            }

            throw new IOException("Unknown response from Master: " + line);
        }
    }
}
