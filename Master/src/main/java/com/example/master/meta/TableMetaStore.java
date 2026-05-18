package com.example.master.meta;

import com.alibaba.fastjson.JSONObject;
import com.example.zookeeper.MetadataManager;
import com.example.zookeeper.RegionNodeInfo;
import com.example.zookeeper.ZookeeperClient;
import org.apache.zookeeper.KeeperException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 表元数据的本地缓存层。
 * 权威数据存于 ZooKeeper 的 /meta/table_region_map（由 MetadataManager 维护）。
 * 启动时一次性加载全量，运行期所有写操作"先写 ZK，再更新内存"。
 */
public final class TableMetaStore {

    private final MetadataManager metadataManager;
    private final ZookeeperClient zkClient;
    private final String metaPath;

    /** table -> regionId。 */
    private final Map<String, String> tableToRegion = new ConcurrentHashMap<>();

    /** regionId -> tables（每个 region 上承载的表）。 */
    private final Map<String, Set<String>> regionToTables = new ConcurrentHashMap<>();

    public TableMetaStore(MetadataManager metadataManager, ZookeeperClient zkClient, String metaPath) {
        this.metadataManager = metadataManager;
        this.zkClient = zkClient;
        this.metaPath = metaPath;
    }

    /**
     * 启动时调用：初始化 ZK 上的元数据节点（不存在则创建），拉取全量到本地缓存。
     */
    public void loadFromZk() throws KeeperException, InterruptedException {
        metadataManager.initMetaNode();
        byte[] data = zkClient.getData(metaPath, null);
        if (data == null || data.length == 0) {
            return;
        }
        JSONObject json = JSONObject.parseObject(new String(data));
        if (json == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : json.entrySet()) {
            String table = entry.getKey();
            Object regionObj = entry.getValue();
            if (regionObj == null) {
                continue;
            }
            String regionId = regionObj.toString();
            tableToRegion.put(table, regionId);
            regionToTables
                    .computeIfAbsent(regionId, k -> ConcurrentHashMap.newKeySet())
                    .add(table);
        }
    }

    public void importFromRegions(String regionsPath) throws KeeperException, InterruptedException {
        if (regionsPath == null || regionsPath.isEmpty()) {
            return;
        }
        if (!zkClient.exists(regionsPath)) {
            return;
        }
        List<String> regions = zkClient.getChildren(regionsPath, null);
        for (String regionId : regions) {
            byte[] data = zkClient.getData(regionsPath + "/" + regionId, null);
            RegionNodeInfo info = RegionNodeInfo.fromBytes(data);
            if (info == null) {
                continue;
            }
            String table = info.getTable();
            if (table == null || table.isEmpty()) {
                continue;
            }
            if (!exists(table)) {
                addTable(table, regionId);
            }
        }
    }

    /** 添加表到指定 region。同时写 ZK。 */
    public void addTable(String table, String regionId) throws KeeperException, InterruptedException {
        metadataManager.updateTableRegion(table, regionId);
        String previous = tableToRegion.put(table, regionId);
        if (previous != null && !previous.equals(regionId)) {
            Set<String> prevSet = regionToTables.get(previous);
            if (prevSet != null) {
                prevSet.remove(table);
            }
        }
        regionToTables
                .computeIfAbsent(regionId, k -> ConcurrentHashMap.newKeySet())
                .add(table);
    }

    /** 删除表的映射。同时写 ZK。 */
    public void removeTable(String table) throws KeeperException, InterruptedException {
        metadataManager.deleteTableRegion(table);
        String regionId = tableToRegion.remove(table);
        if (regionId != null) {
            Set<String> set = regionToTables.get(regionId);
            if (set != null) {
                set.remove(table);
            }
        }
    }

    /** 把 fromRegion 上的所有表批量改挂到 toRegion 上。同时写 ZK。 */
    public List<String> migrateAll(String fromRegion, String toRegion)
            throws KeeperException, InterruptedException {
        Set<String> tables = regionToTables.get(fromRegion);
        if (tables == null || tables.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> moved = new ArrayList<>(tables);
        for (String table : moved) {
            metadataManager.updateTableRegion(table, toRegion);
            tableToRegion.put(table, toRegion);
        }
        Set<String> targetSet = regionToTables
                .computeIfAbsent(toRegion, k -> ConcurrentHashMap.newKeySet());
        targetSet.addAll(moved);
        regionToTables.remove(fromRegion);
        return moved;
    }

    public String getRegionId(String table) {
        return tableToRegion.get(table);
    }

    public boolean exists(String table) {
        return tableToRegion.containsKey(table);
    }

    public Set<String> tablesOf(String regionId) {
        Set<String> set = regionToTables.get(regionId);
        return set == null ? Collections.emptySet() : new HashSet<>(set);
    }

    public int tableCount(String regionId) {
        Set<String> set = regionToTables.get(regionId);
        return set == null ? 0 : set.size();
    }

    /** 测试或调试用：所有表的快照。 */
    public Map<String, String> snapshot() {
        return new java.util.HashMap<>(tableToRegion);
    }
}
