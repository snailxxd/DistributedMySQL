package com.example.regionserver.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * RegionServer startup configuration.
 */
public final class RegionServerConfig {

    private final String regionId;
    private final int port;
    private final String logicalTable;
    private final String physicalTable;
    private final long keyStart;
    private final long keyEnd;
    private final String redirectHostPort;

    private final String zkConnect;
    private final int zkSessionTimeout;
    private final String regionsZNode;
    private final String metaZNode;
    private final String commandsZNode;
    private final String acksZNode;
    private final int regionDefaultPort;

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    private RegionServerConfig(Builder b) {
        this.regionId = b.regionId;
        this.port = b.port;
        this.logicalTable = b.logicalTable;
        this.physicalTable = b.physicalTable;
        this.keyStart = b.keyStart;
        this.keyEnd = b.keyEnd;
        this.redirectHostPort = b.redirectHostPort;
        this.zkConnect = b.zkConnect;
        this.zkSessionTimeout = b.zkSessionTimeout;
        this.regionsZNode = b.regionsZNode;
        this.metaZNode = b.metaZNode;
        this.commandsZNode = b.commandsZNode;
        this.acksZNode = b.acksZNode;
        this.regionDefaultPort = b.regionDefaultPort;
        this.jdbcUrl = b.jdbcUrl;
        this.jdbcUser = b.jdbcUser;
        this.jdbcPassword = b.jdbcPassword;
    }

    public String getRegionId() {
        return regionId;
    }

    public int getPort() {
        return port;
    }

    public String getLogicalTable() {
        return logicalTable;
    }

    public String getPhysicalTable() {
        return physicalTable;
    }

    public long getKeyStart() {
        return keyStart;
    }

    public long getKeyEnd() {
        return keyEnd;
    }

    public String getRedirectHostPort() {
        return redirectHostPort;
    }

    public String getZkConnect() {
        return zkConnect;
    }

    public int getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public String getRegionsZNode() {
        return regionsZNode;
    }

    public String getMetaZNode() {
        return metaZNode;
    }

    public String getCommandsZNode() {
        return commandsZNode;
    }

    public String getAcksZNode() {
        return acksZNode;
    }

    public int getRegionDefaultPort() {
        return regionDefaultPort;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getJdbcUser() {
        return jdbcUser;
    }

    public String getJdbcPassword() {
        return jdbcPassword;
    }

    public void validate() {
        if (logicalTable == null || logicalTable.isEmpty()) {
            throw new IllegalArgumentException("--table is required");
        }
        if (jdbcUrl == null || jdbcUrl.isEmpty()) {
            throw new IllegalArgumentException("--jdbc-url is required");
        }
        if (regionId == null || regionId.isEmpty()) {
            throw new IllegalArgumentException("--region-id is required");
        }
    }

    public static RegionServerConfig fromArgs(String[] args) {
        Builder b = new Builder();
        if (args != null) {
            for (String arg : args) {
                if (arg == null || !arg.startsWith("--")) {
                    continue;
                }
                int eq = arg.indexOf('=');
                if (eq <= 2) {
                    continue;
                }
                String key = arg.substring(2, eq).trim();
                String value = arg.substring(eq + 1).trim();
                applyOverride(b, key, value);
            }
        }
        return b.build();
    }

    private static void applyOverride(Builder b, String key, String value) {
        switch (key) {
            case "region-id" -> b.regionId(value);
            case "port" -> b.port(parseInt(value, b.port));
            case "table" -> b.logicalTable(value);
            case "physical-table" -> b.physicalTable(value);
            case "key-start" -> b.keyStart(parseLong(value, b.keyStart));
            case "key-end" -> b.keyEnd(parseLong(value, b.keyEnd));
            case "redirect" -> b.redirectHostPort(value);
            case "zk" -> b.zkConnect(value);
            case "zk-timeout" -> b.zkSessionTimeout(parseInt(value, b.zkSessionTimeout));
            case "regions-znode" -> b.regionsZNode(value);
            case "meta-znode" -> b.metaZNode(value);
            case "commands-znode" -> b.commandsZNode(value);
            case "acks-znode" -> b.acksZNode(value);
            case "region-default-port" -> b.regionDefaultPort(parseInt(value, b.regionDefaultPort));
            case "jdbc-url" -> b.jdbcUrl(value);
            case "jdbc-user" -> b.jdbcUser(value);
            case "jdbc-password" -> b.jdbcPassword(value);
            case "region-index" -> b.regionIndex(parseInt(value, b.regionIndex));
            default -> { /* ignore */ }
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static long parseLong(String s, long fallback) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final class Builder {
        private String regionId;
        private int port = 5000;
        private String logicalTable;
        private String physicalTable;
        private long keyStart = Long.MIN_VALUE;
        private long keyEnd = Long.MAX_VALUE;
        private String redirectHostPort;

        private String zkConnect = "localhost:2181";
        private int zkSessionTimeout = 5000;
        private String regionsZNode = "/distributed_mysql/rs";
        private String metaZNode = "/meta/table_region_map";
        private String commandsZNode = "/commands";
        private String acksZNode = "/acks";
        private int regionDefaultPort = 5000;

        private String jdbcUrl = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC";
        private String jdbcUser = "root";
        private String jdbcPassword = "";

        private int regionIndex = 0;

        public Builder regionId(String v) { this.regionId = v; return this; }
        public Builder port(int v) { this.port = v; return this; }
        public Builder logicalTable(String v) { this.logicalTable = v; return this; }
        public Builder physicalTable(String v) { this.physicalTable = v; return this; }
        public Builder keyStart(long v) { this.keyStart = v; return this; }
        public Builder keyEnd(long v) { this.keyEnd = v; return this; }
        public Builder redirectHostPort(String v) { this.redirectHostPort = v; return this; }
        public Builder zkConnect(String v) { this.zkConnect = v; return this; }
        public Builder zkSessionTimeout(int v) { this.zkSessionTimeout = v; return this; }
        public Builder regionsZNode(String v) { this.regionsZNode = v; return this; }
        public Builder metaZNode(String v) { this.metaZNode = v; return this; }
        public Builder commandsZNode(String v) { this.commandsZNode = v; return this; }
        public Builder acksZNode(String v) { this.acksZNode = v; return this; }
        public Builder regionDefaultPort(int v) { this.regionDefaultPort = v; return this; }
        public Builder jdbcUrl(String v) { this.jdbcUrl = v; return this; }
        public Builder jdbcUser(String v) { this.jdbcUser = v; return this; }
        public Builder jdbcPassword(String v) { this.jdbcPassword = v; return this; }
        public Builder regionIndex(int v) { this.regionIndex = v; return this; }

        public RegionServerConfig build() {
            if (this.physicalTable == null || this.physicalTable.isEmpty()) {
                this.physicalTable = defaultPhysicalTable(this.logicalTable, this.regionIndex);
            }
            if (this.regionId == null || this.regionId.isEmpty()) {
                this.regionId = defaultRegionId(this.port);
            }
            return new RegionServerConfig(this);
        }

        private static String defaultPhysicalTable(String logicalTable, int regionIndex) {
            if (logicalTable == null || logicalTable.isEmpty()) {
                return null;
            }
            return logicalTable;
        }
    }

    private static String defaultRegionId(int port) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            return "node_" + host + ":" + port;
        } catch (UnknownHostException e) {
            return "node_unknown:" + port;
        }
    }
}
