package com.example.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RegionServerClient {

    private final int timeoutMillis;

    public RegionServerClient() {
        this(3000);
    }

    public RegionServerClient(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 发送一行原始命令到 RegionServer，返回一行响应
     * @param location RegionServer 地址
     * @param command 未经处理的原始 SQL 命令
     * @return RegionServer 响应的原始字符串
     * @throws IOException
     */
    public String send(RegionLocation location, String command) throws IOException {
        if (location == null) {
            throw new IOException("Region location is null");
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(location.getHost(), location.getPort()), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            writer.write(command + "\n");
            writer.flush();

            String firstLine = reader.readLine();
            if (firstLine == null) {
                throw new IOException("Empty response from RegionServer");
            }
            if (!"OK".equals(firstLine)) {
                return firstLine.trim();
            }

            StringBuilder response = new StringBuilder();
            response.append(firstLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if ("END".equals(line)) {
                    response.append("\n").append(line);
                    break;
                }
                response.append("\n").append(line);
            }
            return response.toString();
        }
    }
}
