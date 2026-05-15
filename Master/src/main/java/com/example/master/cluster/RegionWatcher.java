package com.example.master.cluster;

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
 * 监听 /region_servers 子节点变化，把"新增/消失"事件转交给上层回调。
 *
 * <p>ZooKeeper 原生 Watcher 是一次性的，每次回调中都要重新注册。
 * 这里使用单线程 Executor 串行处理，避免事件并发引发的乱序。
 */
public final class RegionWatcher {

    public interface RegionListener {
        /** 子节点新增：传入 regionId 与解析出来的 host。 */
        void onRegionAdded(String regionId, String host);

        /** 子节点消失：传入 regionId。 */
        void onRegionRemoved(String regionId);
    }

    private final ZookeeperClient zkClient;
    private final String regionsPath;
    private final RegionListener listener;

    private final ExecutorService worker;
    private final Set<String> known = new HashSet<>();
    private volatile boolean running = false;

    public RegionWatcher(ZookeeperClient zkClient, String regionsPath, RegionListener listener) {
        this.zkClient = zkClient;
        this.regionsPath = regionsPath;
        this.listener = listener;
        this.worker = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "master-region-watcher");
            t.setDaemon(true);
            return t;
        });
    }

    public synchronized void start() throws KeeperException, InterruptedException {
        if (running) {
            return;
        }
        running = true;
        if (!zkClient.exists(regionsPath)) {
            zkClient.createPersistentNode(regionsPath, "".getBytes());
        }
        refreshAndRegister();
    }

    public synchronized void stop() {
        running = false;
        worker.shutdownNow();
    }

    /** 拉取一次子节点列表，对照本地集合做 diff，最后重新注册 watcher。 */
    private void refreshAndRegister() {
        List<String> children;
        try {
            children = zkClient.getChildren(regionsPath, this::onZkEvent);
        } catch (KeeperException | InterruptedException e) {
            System.err.println("RegionWatcher refresh failed: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        Set<String> current = new HashSet<>(children);

        Set<String> added = new HashSet<>(current);
        added.removeAll(known);
        Set<String> removed = new HashSet<>(known);
        removed.removeAll(current);

        for (String regionId : removed) {
            safeOnRemoved(regionId);
        }
        for (String regionId : added) {
            String host = readHost(regionId);
            safeOnAdded(regionId, host);
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

    /** 读取子节点数据，约定格式为 "serverId:host"。 */
    private String readHost(String regionId) {
        String path = regionsPath + "/" + regionId;
        try {
            byte[] data = zkClient.getData(path, null);
            if (data == null || data.length == 0) {
                return null;
            }
            String s = new String(data);
            int idx = s.indexOf(':');
            return idx >= 0 ? s.substring(idx + 1).trim() : s.trim();
        } catch (KeeperException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    private void safeOnAdded(String regionId, String host) {
        try {
            listener.onRegionAdded(regionId, host);
        } catch (RuntimeException ex) {
            System.err.println("onRegionAdded error: " + ex.getMessage());
        }
    }

    private void safeOnRemoved(String regionId) {
        try {
            listener.onRegionRemoved(regionId);
        } catch (RuntimeException ex) {
            System.err.println("onRegionRemoved error: " + ex.getMessage());
        }
    }
}
