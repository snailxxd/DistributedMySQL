package com.example.zookeeper;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class ClusterManager {
    private final ZookeeperClient zkClient;
    private final String serverId;
    private final String basePath;
    private final String nodePath;
    private final String serverAddress;
    private final String logicalTable;
    private final Integer port;

    public ClusterManager(ZookeeperClient zkClient, String serverId, String nodePath) {
        this(zkClient, serverId, nodePath, null, null);
    }

    public ClusterManager(ZookeeperClient zkClient, String serverId, String nodePath, String logicalTable) {
        this(zkClient, serverId, nodePath, logicalTable, null);
    }

    public ClusterManager(ZookeeperClient zkClient, String serverId, String nodePath, String logicalTable, Integer port) {
        this.zkClient = zkClient;
        this.serverId = serverId;
        this.basePath = nodePath;
        this.nodePath = nodePath + "/" + serverId;
        this.logicalTable = logicalTable;
        this.port = port;
        try {
            this.serverAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            throw new RuntimeException("无法获取本机地址", e);
        }
    }

    /**
     * 注册临时节点（RegionServer 启动时调用）
     */
    public void register() throws KeeperException, InterruptedException {
        zkClient.createPersistentPath(basePath);
        RegionNodeInfo info = new RegionNodeInfo(serverId, serverAddress, port, logicalTable);
        byte[] data = info.toBytes();
        zkClient.createEphemeralNode(nodePath, data);
        System.out.println("注册成功: " + nodePath + " -> " + serverAddress);
    }

    /**
     * 注销节点（RegionServer 关闭时调用）
     */
    public void unregister() throws KeeperException, InterruptedException {
        if (zkClient.exists(nodePath)) {
            zkClient.deleteNode(nodePath);
            System.out.println("注销成功: " + nodePath);
        }
    }

    /**
     * 监听 RegionServer 列表变化（Master 调用）
     * @param callback 当服务器列表变化时的回调函数
     */
    public void watchRegionServers(Runnable callback) throws KeeperException, InterruptedException {
        zkClient.getChildren(basePath, event -> {
            if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                System.out.println("RegionServer 列表发生变化");
                callback.run();
            }
        });
    }

    /**
     * 获取所有存活的 RegionServer 列表
     */
    public List<String> getActiveRegionServers() throws KeeperException, InterruptedException {
        return zkClient.getChildren(basePath, null);
    }

    public String getServerId() {
        return serverId;
    }
}