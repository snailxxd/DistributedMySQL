package com.example.zookeeper;

import org.apache.zookeeper.KeeperException;
import com.alibaba.fastjson.JSONObject;

public class MetadataManager {
    private final ZookeeperClient zkClient;
    private static final String META_PATH = "/meta/table_region_map";
    private static final String META_PARENT_PATH = "/meta";

    public MetadataManager(ZookeeperClient zkClient) {
        this.zkClient = zkClient;
    }

    public void initMetaNode() throws KeeperException, InterruptedException {
        // 先创建父节点 /meta
        zkClient.createPersistentNode(META_PARENT_PATH, "".getBytes());
        // 再创建元数据节点
        zkClient.createPersistentNode(META_PATH, "{}".getBytes());
    }

    public void updateTableRegion(String tableName, String regionServerId) throws KeeperException, InterruptedException {
        // 确保节点存在
        if (!zkClient.exists(META_PATH)) {
            initMetaNode();
        }
        byte[] data = zkClient.getData(META_PATH, null);
        String currentData = (data == null || data.length == 0) ? "{}" : new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        meta.put(tableName, regionServerId);
        zkClient.setData(META_PATH, meta.toJSONString().getBytes());
    }

    public String getRegionServerForTable(String tableName) throws KeeperException, InterruptedException {
        if (!zkClient.exists(META_PATH)) {
            return null;
        }
        byte[] data = zkClient.getData(META_PATH, null);
        if (data == null || data.length == 0) {
            return null;
        }
        String currentData = new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        return meta.getString(tableName);
    }

    public void deleteTableRegion(String tableName) throws KeeperException, InterruptedException {
        if (!zkClient.exists(META_PATH)) {
            return;
        }
        byte[] data = zkClient.getData(META_PATH, null);
        if (data == null || data.length == 0) {
            return;
        }
        String currentData = new String(data);
        JSONObject meta = JSONObject.parseObject(currentData);
        meta.remove(tableName);
        zkClient.setData(META_PATH, meta.toJSONString().getBytes());
    }

    public boolean tableExists(String tableName) throws KeeperException, InterruptedException {
        return getRegionServerForTable(tableName) != null;
    }
}