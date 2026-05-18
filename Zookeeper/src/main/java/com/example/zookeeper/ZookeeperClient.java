package com.example.zookeeper;

import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.CreateMode;
import java.io.IOException;
import java.util.List;

public class ZookeeperClient {
    private ZooKeeper zk;
    private final String connectionString;
    private final int sessionTimeout;

    public ZookeeperClient(String connectionString, int sessionTimeout) {
        this.connectionString = connectionString;
        this.sessionTimeout = sessionTimeout;
    }

    public void connect() throws IOException {
        zk = new ZooKeeper(connectionString, sessionTimeout, event -> {
            System.out.println("ZooKeeper 状态: " + event.getState());
        });
    }

    public ZooKeeper getZooKeeper() {
        return zk;
    }

    public void createEphemeralNode(String path, byte[] data) throws KeeperException, InterruptedException {
        zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
    }

    public void createPersistentNode(String path, byte[] data) throws KeeperException, InterruptedException {
        if (zk.exists(path, false) == null) {
            zk.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    public void createPersistentPath(String path) throws KeeperException, InterruptedException {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return;
        }
        String[] parts = path.split("/");
        StringBuilder current = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            current.append('/').append(part);
            String currentPath = current.toString();
            if (zk.exists(currentPath, false) == null) {
                zk.create(currentPath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        }
    }

    public List<String> getChildren(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return zk.getChildren(path, watcher);
    }

    public byte[] getData(String path, Watcher watcher) throws KeeperException, InterruptedException {
        return zk.getData(path, watcher, null);
    }

    public void setData(String path, byte[] data) throws KeeperException, InterruptedException {
        zk.setData(path, data, -1);
    }

    public void deleteNode(String path) throws KeeperException, InterruptedException {
        zk.delete(path, -1);
    }

    public boolean exists(String path) throws KeeperException, InterruptedException {
        return zk.exists(path, false) != null;
    }

    public void close() throws InterruptedException {
        if (zk != null) {
            zk.close();
        }
    }
}