package com.example.master.cluster;

/**
 * RegionServer 节点信息。
 * regionId 来自 ZooKeeper 子节点名（与 ClusterManager 写入的 serverId 一致），
 * host 来自 ZooKeeper 节点数据 "serverId:host"。
 * port 来自 Master 配置 region.server.port。
 */
public final class RegionNode {

    public enum Status {
        /** 已被发现且当前在线。 */
        ACTIVE,
        /** 曾经在线，目前认为已失效。 */
        INVALID
    }

    private final String regionId;
    private final String host;
    private final int port;
    private volatile Status status;

    public RegionNode(String regionId, String host, int port, Status status) {
        this.regionId = regionId;
        this.host = host;
        this.port = port;
        this.status = status;
    }

    public String getRegionId() {
        return regionId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String toHostPort() {
        return host + ":" + port;
    }

    @Override
    public String toString() {
        return "RegionNode{" + regionId + " " + host + ":" + port + " " + status + "}";
    }
}
