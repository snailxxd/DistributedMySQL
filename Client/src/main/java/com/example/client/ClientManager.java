package com.example.client;

import com.example.client.sql.SqlParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientManager {

    private final RegionCache regionCache;
    private final MasterClient masterClient;
    private final RegionServerClient regionServerClient;
    private final SqlParser sqlParser;

    /**
     * 构造函数
     *
     * @param masterHost Master 主机名或 IP
     * @param masterPort Master 端口
     */
    public ClientManager(String masterHost, int masterPort) {
        this.regionCache = new RegionCache();
        this.masterClient = new MasterClient(masterHost, masterPort);
        this.regionServerClient = new RegionServerClient();
        this.sqlParser = new SqlParser();
    }

    /**
     * 客户端线程主循环，接收用户请求并响应。
     */
    public void run() {
        System.out.println("Client started. SQL: SELECT/INSERT/UPDATE/DELETE/CREATE/DROP, exit");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    System.out.println("Input error: " + e.getMessage());
                    break;
                }
                if (line == null) {
                    System.out.println("Input closed. Exit.");
                    break;
                }
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if ("exit".equalsIgnoreCase(line) || "quit".equalsIgnoreCase(line)) {
                    break;
                }

                handleSql(line);
            }
        } catch (IOException e) {
            System.out.println("Input error: " + e.getMessage());
        }
        System.out.println("Client stopped.");
    }

    /**
     * 处理用户输入的 SQL 语句并路由到 Master 或 RegionServer。
     * @param sql 原始 SQL 语句
     */
    private void handleSql(String sql) {
        SqlParser.ParseResult parsed = sqlParser.parse(sql);
        if (parsed.getNormalizedSql().isEmpty()) {
            return;
        }

        if (parsed.isCreateOrDrop()) {
            String masterResponse = sendToMaster(parsed.getNormalizedSql());
            System.out.println(masterResponse);
            return;
        }

        if (parsed.getTable() == null) {
            System.out.println("ERROR: cannot route SQL without table");
            return;
        }

        String response = sendToRegion(parsed.getTable(), parsed.getNormalizedSql());
        System.out.println(response);
    }

    /**
     * 发送 SQL 到 Master，返回原始响应行。
     */
    private String sendToMaster(String sql) {
        try {
            return "Master>> " + masterClient.sendSql(sql);
        } catch (IOException e) {
            return "ERR " + e.getMessage();
        }
    }

    /**
     * 向 Master 请求表对应的默认 Region。
     */
    private RegionLocation locateTableFromMaster(String table) {
        try {
            return masterClient.locateTable(table);
        } catch (IOException e) {
            System.out.println("Master table locate failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 发送 SQL 到 RegionServer，必要时更新缓存并重试一次。
     */
    private String sendToRegion(String table, String commandLine) {
        RegionLocation location = regionCache.find(table);
        if (location == null) {
            location = locateTableFromMaster(table);
            if (location == null) {
                return "ERR Master locate failed";
            }
            regionCache.set(table, location);
        }

        String response = sendRaw(location, commandLine);
        if (response.startsWith("MOVED ")) {
            RegionLocation redirect = RegionLocation.parse(response.substring(6).trim());
            if (redirect != null) {
                regionCache.set(table, redirect);
                response = sendRaw(redirect, commandLine);
            }
        }
        return prefixLines("RegionServer>> ", response);
    }

    private String prefixLines(String prefix, String text) {
        if (text == null || text.isEmpty()) {
            return prefix.trim();
        }
        String[] lines = text.split("\r?\n");
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                out.append('\n');
            }
            out.append(prefix).append(lines[i]);
        }
        return out.toString();
    }

    /**
     * 发送原始命令到 RegionServer。
     */
    private String sendRaw(RegionLocation location, String commandLine) {
        try {
            return regionServerClient.send(location, commandLine);
        } catch (IOException e) {
            return "ERR " + e.getMessage();
        }
    }
}
