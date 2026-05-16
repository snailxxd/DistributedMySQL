package com.example.zookeeper;

import org.apache.zookeeper.KeeperException;
import java.util.List;

//供客户端或 Master 使用，获取当前存活的 RegionServer 列表。
public class ServiceDiscovery {
    private static final String DEFAULT_REGIONS_PATH = "/region_servers";
    private final ZookeeperClient zkClient;
    private final String regionsPath;

    public ServiceDiscovery(ZookeeperClient zkClient) {
        this(zkClient, DEFAULT_REGIONS_PATH);
    }

    public ServiceDiscovery(ZookeeperClient zkClient, String regionsPath) {
        this.zkClient = zkClient;
        this.regionsPath = (regionsPath == null || regionsPath.isEmpty())
                ? DEFAULT_REGIONS_PATH
                : regionsPath;
    }

    public List<String> getActiveRegionServers() throws KeeperException, InterruptedException {
        return zkClient.getChildren(regionsPath, null);
    }
}