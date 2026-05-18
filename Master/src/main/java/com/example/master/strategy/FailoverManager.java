package com.example.master.strategy;

import com.example.master.balancer.LoadBalancer;
import com.example.master.cluster.RegionNode;
import com.example.master.cluster.RegionRegistry;
import com.example.master.cluster.RegionWatcher;
import com.example.master.command.CommandDispatcher;
import com.example.master.config.MasterConfig;
import com.example.master.meta.TableMetaStore;

import java.util.List;

/**
 * 集群事件处理：响应 RegionWatcher 上下线事件，执行 DISCOVER / RECOVER / INVALID 三类策略。
 *
 * <ul>
 *   <li>DISCOVER：新节点入册（无表，立刻可被负载均衡选中）</li>
 *   <li>RECOVER：曾失效又回来，下发 CLEAR 指令清空本地脏数据后入活跃池</li>
 *   <li>INVALID：节点失效，挑选活跃中表数最少者接管它的全部表，更新元数据，
 *       并向接管者下发 MIGRATE 指令</li>
 * </ul>
 */
public final class FailoverManager implements RegionWatcher.RegionListener {

    private final RegionRegistry registry;
    private final TableMetaStore metaStore;
    private final LoadBalancer balancer;
    private final CommandDispatcher dispatcher;
    private final int regionDefaultPort;

    public FailoverManager(RegionRegistry registry,
                           TableMetaStore metaStore,
                           LoadBalancer balancer,
                           CommandDispatcher dispatcher,
                           MasterConfig config) {
        this.registry = registry;
        this.metaStore = metaStore;
        this.balancer = balancer;
        this.dispatcher = dispatcher;
        this.regionDefaultPort = config.getRegionDefaultPort();
    }

    @Override
    public synchronized void onRegionAdded(String regionId, String host, Integer port, String table) {
        ensureTableMapping(regionId, table);
        if (registry.isKnown(regionId)) {
            recover(regionId, host, port);
        } else {
            discover(regionId, host, port);
        }
    }

    @Override
    public synchronized void onRegionRemoved(String regionId) {
        invalid(regionId);
    }

    /** 第一次见到的节点：直接入册。 */
    private void discover(String regionId, String host, Integer port) {
        if (host == null || host.isEmpty()) {
            System.err.println("DISCOVER skipped, missing host for " + regionId);
            return;
        }
        RegionNode node = registry.addNew(regionId, host, resolvePort(port));
        System.out.println("DISCOVER: " + node);
    }

    private int resolvePort(Integer port) {
        return (port != null && port > 0) ? port : regionDefaultPort;
    }

    private void ensureTableMapping(String regionId, String table) {
        if (table == null || table.isEmpty()) {
            return;
        }
        if (metaStore.exists(table)) {
            return;
        }
        try {
            metaStore.addTable(table, regionId);
            System.out.println("IMPORTED: " + table + " -> " + regionId);
        } catch (Exception e) {
            System.err.println("IMPORT failed for " + table + ": " + e.getMessage());
        }
    }

    /**
     * 历史节点重新上线：
     * 此时该节点本地数据已经被认为是脏的（容灾期间它的表已迁走），
     * 通过 ZK 指令通道下发 CLEAR，让它清空本地存储，并恢复为活跃。
     */
    private void recover(String regionId, String host, Integer port) {
        int resolvedPort = resolvePort(port);
        if (host != null && !host.isEmpty()) {
            registry.markActive(regionId, host, resolvedPort);
        } else {
            registry.markActive(regionId,
                    registry.get(regionId).getHost(),
                    resolvedPort);
        }
        System.out.println("RECOVER: " + registry.get(regionId));
        try {
            dispatcher.sendClear(regionId);
        } catch (Exception e) {
            System.err.println("RECOVER dispatch CLEAR failed: " + e.getMessage());
        }
    }

    /**
     * 节点失效：
     * 1. 标记 INVALID
     * 2. 在剩余活跃节点中挑表数最少的作为接管者
     * 3. 把元数据上 fromRegion 的所有表改挂到目标 region
     * 4. 给目标 region 下发 MIGRATE 指令，告知应接管的表清单
     */
    private void invalid(String regionId) {
        RegionNode node = registry.markInvalid(regionId);
        if (node == null) {
            System.err.println("INVALID on unknown region: " + regionId);
            return;
        }
        System.out.println("INVALID: " + node);

        List<String> tablesOnDead;
        try {
            tablesOnDead = new java.util.ArrayList<>(metaStore.tablesOf(regionId));
        } catch (Exception e) {
            System.err.println("INVALID read tables failed: " + e.getMessage());
            return;
        }
        if (tablesOnDead.isEmpty()) {
            System.out.println("INVALID: no tables to migrate from " + regionId);
            return;
        }

        RegionNode target = balancer.pickExcluding(regionId);
        if (target == null) {
            System.err.println("INVALID: no available region to take over from " + regionId);
            return;
        }

        try {
            List<String> moved = metaStore.migrateAll(regionId, target.getRegionId());
            System.out.println("INVALID migrate " + moved + " from "
                    + regionId + " to " + target.getRegionId());
            dispatcher.sendMigrate(target.getRegionId(), regionId, moved);
        } catch (Exception e) {
            System.err.println("INVALID migrate failed: " + e.getMessage());
        }
    }
}
