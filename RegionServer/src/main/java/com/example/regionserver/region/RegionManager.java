package com.example.regionserver.region;

import com.example.regionserver.config.RegionServerConfig;
import com.example.zookeeper.MetadataManager;
import com.example.zookeeper.RegionNodeInfo;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.KeeperException;

/**
 * Tracks the local region ownership and resolves redirects for foreign tables/keys.
 */
public final class RegionManager {

    public enum RouteStatus {
        ACCEPT,
        MOVED,
        ERROR
    }

    public static final class RouteResult {
        public final RouteStatus status;
        public final String message;

        private RouteResult(RouteStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public static RouteResult accept() {
            return new RouteResult(RouteStatus.ACCEPT, null);
        }

        public static RouteResult moved(String hostPort) {
            return new RouteResult(RouteStatus.MOVED, hostPort);
        }

        public static RouteResult error(String msg) {
            return new RouteResult(RouteStatus.ERROR, msg);
        }
    }

    private final RegionServerConfig config;
    private final ZookeeperClient zkClient;
    private final MetadataManager metadataManager;

    public RegionManager(RegionServerConfig config, ZookeeperClient zkClient, MetadataManager metadataManager) {
        this.config = config;
        this.zkClient = zkClient;
        this.metadataManager = metadataManager;
    }

    public String getLogicalTable() {
        return config.getLogicalTable();
    }

    public String getPhysicalTable() {
        return config.getPhysicalTable();
    }

    public String getRegionId() {
        return config.getRegionId();
    }

    public RouteResult route(String table, Long key) {
        if (table == null || table.isEmpty()) {
            return RouteResult.error("ERR missing table");
        }
        if (!table.equalsIgnoreCase(config.getLogicalTable())) {
            String owner = resolveOwner(table);
            if (owner == null) {
                return RouteResult.error("ERR table not found: " + table);
            }
            return RouteResult.moved(owner);
        }
        if (key != null && !inRange(key)) {
            if (config.getRedirectHostPort() != null && !config.getRedirectHostPort().isEmpty()) {
                return RouteResult.moved(config.getRedirectHostPort());
            }
            return RouteResult.error("ERR key out of region");
        }
        return RouteResult.accept();
    }

    private boolean inRange(long key) {
        return key >= config.getKeyStart() && key <= config.getKeyEnd();
    }

    private String resolveOwner(String table) {
        try {
            String regionId = metadataManager.getRegionServerForTable(table);
            if (regionId == null) {
                return null;
            }
            return resolveHostPort(regionId);
        } catch (KeeperException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private String resolveHostPort(String regionId) throws KeeperException, InterruptedException {
        String path = config.getRegionsZNode() + "/" + regionId;
        byte[] data = zkClient.getData(path, null);
        RegionNodeInfo info = RegionNodeInfo.fromBytes(data);
        if (info == null || info.getHost() == null || info.getHost().isEmpty()) {
            return null;
        }
        int port = (info.getPort() != null && info.getPort() > 0)
                ? info.getPort()
                : config.getRegionDefaultPort();
        return info.getHost() + ":" + port;
    }
}
