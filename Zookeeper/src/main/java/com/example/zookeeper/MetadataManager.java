package com.example.zookeeper;

import org.apache.zookeeper.KeeperException;
import com.alibaba.fastjson.JSONObject;

public class MetadataManager {
    private final ZookeeperClient zkClient;
    private final String metaPath;
    private final String metaParentPath;
    private static final String DEFAULT_META_PATH = "/meta/table_region_map";

    public MetadataManager(ZookeeperClient zkClient) {
        this(zkClient, DEFAULT_META_PATH);
    }

    public MetadataManager(ZookeeperClient zkClient, String metaPath) {
        this.zkClient = zkClient;
        this.metaPath = (metaPath == null || metaPath.isEmpty()) ? DEFAULT_META_PATH : metaPath;
        int lastSlash = this.metaPath.lastIndexOf('/');
        this.metaParentPath = (lastSlash > 0) ? this.metaPath.substring(0, lastSlash) : "/meta";
    }

    public void initMetaNode() throws KeeperException, InterruptedException {
        // 先创建父节点
        zkClient.createPersistentNode(metaParentPath, "".getBytes());
        // 再创建元数据节点
        zkClient.createPersistentNode(metaPath, "{}".getBytes());
    }

    public void updateTableRegion(String tableName, String regionServerId) throws KeeperException, InterruptedException {
        // 确保节点存在
        if (!zkClient.exists(metaPath)) {
            initMetaNode();
        }
        byte[] data = zkClient.getData(metaPath, null);
        String currentData = (data == null || data.length == 0) ? "{}" : new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        meta.put(tableName, regionServerId);
        zkClient.setData(metaPath, meta.toJSONString().getBytes());
    }

    public String getRegionServerForTable(String tableName) throws KeeperException, InterruptedException {
        if (!zkClient.exists(metaPath)) {
            return null;
        }
        byte[] data = zkClient.getData(metaPath, null);
        if (data == null || data.length == 0) {
            return null;
        }
        String currentData = new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        return meta.getString(tableName);
    }

    public void deleteTableRegion(String tableName) throws KeeperException, InterruptedException {
        if (!zkClient.exists(metaPath)) {
            return;
        }
        byte[] data = zkClient.getData(metaPath, null);
        if (data == null || data.length == 0) {
            return;
        }
        String currentData = new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        meta.remove(tableName);
        zkClient.setData(metaPath, meta.toJSONString().getBytes());
    }

    public boolean tableExists(String tableName) throws KeeperException, InterruptedException {
        return getRegionServerForTable(tableName) != null;
    }
}