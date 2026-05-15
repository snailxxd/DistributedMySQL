package com.example.master.config;

/**
 * Master 启动配置。
 * 默认值在此集中维护，命令行参数或环境变量可覆盖。
 */
public final class MasterConfig {

    /** Master 对客户端开放的端口，与 Client 模块默认协议一致。 */
    private final int clientPort;

    /** ZooKeeper 连接串，例如 localhost:2181。 */
    private final String zkConnect;

    /** ZooKeeper 会话超时（毫秒）。 */
    private final int zkSessionTimeout;

    /**
     * RegionServer 对外服务端口。
     * 因为 Zookeeper 模块的 ClusterManager.register() 只把 host 写入 znode，
     * 这里假定所有 RegionServer 使用同一对外端口，由 Master 拼接成 host:port。
     */
    private final int regionDefaultPort;

    /** RegionServer 注册根节点。与 Zookeeper 模块约定一致。 */
    private final String regionsZNode;

    /** 表-Region 映射节点。与 MetadataManager 内部使用的常量一致。 */
    private final String metaZNode;

    /** Master 给 RegionServer 下发指令的根目录。 */
    private final String commandsZNode;

    /** RegionServer 回写执行回执的根目录。 */
    private final String acksZNode;

    private MasterConfig(Builder b) {
        this.clientPort = b.clientPort;
        this.zkConnect = b.zkConnect;
        this.zkSessionTimeout = b.zkSessionTimeout;
        this.regionDefaultPort = b.regionDefaultPort;
        this.regionsZNode = b.regionsZNode;
        this.metaZNode = b.metaZNode;
        this.commandsZNode = b.commandsZNode;
        this.acksZNode = b.acksZNode;
    }

    public int getClientPort() {
        return clientPort;
    }

    public String getZkConnect() {
        return zkConnect;
    }

    public int getZkSessionTimeout() {
        return zkSessionTimeout;
    }

    public int getRegionDefaultPort() {
        return regionDefaultPort;
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

    /**
     * 解析命令行参数，未传入的项使用默认值。
     * 支持格式：--key=value
     */
    public static MasterConfig fromArgs(String[] args) {
        Builder b = new Builder();
        if (args == null) {
            return b.build();
        }
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
        return b.build();
    }

    private static void applyOverride(Builder b, String key, String value) {
        switch (key) {
            case "client-port" -> b.clientPort(parseInt(value, b.clientPort));
            case "zk" -> b.zkConnect(value);
            case "zk-timeout" -> b.zkSessionTimeout(parseInt(value, b.zkSessionTimeout));
            case "region-port" -> b.regionDefaultPort(parseInt(value, b.regionDefaultPort));
            case "regions-znode" -> b.regionsZNode(value);
            case "meta-znode" -> b.metaZNode(value);
            case "commands-znode" -> b.commandsZNode(value);
            case "acks-znode" -> b.acksZNode(value);
            default -> { /* 忽略未知参数 */ }
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static final class Builder {
        private int clientPort = 12345;
        private String zkConnect = "localhost:2181";
        private int zkSessionTimeout = 5000;
        private int regionDefaultPort = 5000;
        private String regionsZNode = "/region_servers";
        private String metaZNode = "/meta/table_region_map";
        private String commandsZNode = "/commands";
        private String acksZNode = "/acks";

        public Builder clientPort(int v) { this.clientPort = v; return this; }
        public Builder zkConnect(String v) { this.zkConnect = v; return this; }
        public Builder zkSessionTimeout(int v) { this.zkSessionTimeout = v; return this; }
        public Builder regionDefaultPort(int v) { this.regionDefaultPort = v; return this; }
        public Builder regionsZNode(String v) { this.regionsZNode = v; return this; }
        public Builder metaZNode(String v) { this.metaZNode = v; return this; }
        public Builder commandsZNode(String v) { this.commandsZNode = v; return this; }
        public Builder acksZNode(String v) { this.acksZNode = v; return this; }

        public MasterConfig build() {
            return new MasterConfig(this);
        }
    }
}
