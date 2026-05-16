package com.example.regionserver.service;

import com.alibaba.fastjson.JSONObject;
import com.example.regionserver.region.RegionManager;
import com.example.regionserver.storage.MySQLStorageEngine;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Watches /commands/<regionId> and executes simple CLEAR/MIGRATE actions.
 */
public final class RegionCommandListener {

    private final ZookeeperClient zkClient;
    private final String commandsRoot;
    private final String acksRoot;
    private final String regionId;
    private final RegionManager regionManager;
    private final MySQLStorageEngine storageEngine;

    private final ExecutorService worker;
    private final Set<String> known = new HashSet<>();
    private volatile boolean running = false;

    public RegionCommandListener(ZookeeperClient zkClient,
                                 String commandsRoot,
                                 String acksRoot,
                                 String regionId,
                                 RegionManager regionManager,
                                 MySQLStorageEngine storageEngine) {
        this.zkClient = zkClient;
        this.commandsRoot = commandsRoot;
        this.acksRoot = acksRoot;
        this.regionId = regionId;
        this.regionManager = regionManager;
        this.storageEngine = storageEngine;
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "region-command-listener");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() throws KeeperException, InterruptedException {
        if (running) {
            return;
        }
        running = true;
        ensurePaths();
        refreshAndRegister();
    }

    public synchronized void stop() {
        running = false;
        worker.shutdownNow();
    }

    private void ensurePaths() throws KeeperException, InterruptedException {
        zkClient.createPersistentNode(commandsRoot, "".getBytes());
        zkClient.createPersistentNode(acksRoot, "".getBytes());
        zkClient.createPersistentNode(commandsRoot + "/" + regionId, "".getBytes());
        zkClient.createPersistentNode(acksRoot + "/" + regionId, "".getBytes());
    }

    private void refreshAndRegister() {
        List<String> children;
        try {
            children = zkClient.getChildren(commandsRoot + "/" + regionId, this::onZkEvent);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("Command listener refresh failed: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        Set<String> current = new HashSet<>(children);
        Set<String> added = new HashSet<>(current);
        added.removeAll(known);

        for (String cmdId : added) {
            handleCommand(cmdId);
        }

        known.clear();
        known.addAll(current);
    }

    private void onZkEvent(WatchedEvent event) {
        if (!running) {
            return;
        }
        if (event.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
            worker.submit(this::refreshAndRegister);
        }
    }

    private void handleCommand(String cmdId) {
        String path = commandsRoot + "/" + regionId + "/" + cmdId;
        try {
            byte[] data = zkClient.getData(path, null);
            if (data == null || data.length == 0) {
                return;
            }
            JSONObject body = JSONObject.parseObject(new String(data));
            String type = body.getString("type");
            if ("CLEAR".equalsIgnoreCase(type)) {
                storageEngine.clearTable(regionManager.getPhysicalTable());
            } else if ("MIGRATE".equalsIgnoreCase(type)) {
                // In this simplified RS, data migration is a no-op placeholder.
            }
            writeAck(cmdId, "OK");
        } catch (Exception e) {
            writeAck(cmdId, "ERR " + e.getMessage());
        }
    }

    private void writeAck(String cmdId, String status) {
        String ackPath = acksRoot + "/" + regionId + "/" + cmdId;
        JSONObject body = new JSONObject();
        body.put("id", cmdId);
        body.put("status", status);
        body.put("ts", System.currentTimeMillis());
        try {
            zkClient.createPersistentNode(ackPath, body.toJSONString().getBytes());
        } catch (Exception e) {
            System.err.println("Write ack failed: " + e.getMessage());
        }
    }
}

