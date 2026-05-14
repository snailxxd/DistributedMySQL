package com.example.zookeeper;

import org.apache.zookeeper.KeeperException;
import java.util.List;

//供客户端或 Master 使用，获取当前存活的 RegionServer 列表。
public class ServiceDiscovery {
    private final ZookeeperClient zkClient;

    public ServiceDiscovery(ZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    public List<String> getActiveRegionServers() throws KeeperException, InterruptedException {
        return zkClient.getChildren("/region_servers", null);
    }
}