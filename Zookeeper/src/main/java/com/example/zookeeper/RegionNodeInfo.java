package com.example.zookeeper;

import com.alibaba.fastjson.JSONObject;

public final class RegionNodeInfo {
    private final String regionId;
    private final String host;
    private final Integer port;
    private final String table;

    public RegionNodeInfo(String regionId, String host, Integer port, String table) {
        this.regionId = regionId;
        this.host = host;
        this.port = port;
        this.table = table;
    }

    public RegionNodeInfo(String regionId, String host, String table) {
        this(regionId, host, null, table);
    }

    public String getRegionId() {
        return regionId;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getTable() {
        return table;
    }

    public byte[] toBytes() {
        JSONObject body = new JSONObject();
        if (regionId != null && !regionId.isEmpty()) {
            body.put("id", regionId);
        }
        if (host != null && !host.isEmpty()) {
            body.put("host", host);
        }
        if (port != null && port > 0) {
            body.put("port", port);
        }
        if (table != null && !table.isEmpty()) {
            body.put("table", table);
        }
        return body.toJSONString().getBytes();
    }

    public static RegionNodeInfo fromBytes(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        String raw = new String(data).trim();
        if (raw.isEmpty()) {
            return null;
        }
        if (raw.startsWith("{")) {
            JSONObject body = JSONObject.parseObject(raw);
            if (body == null) {
                return null;
            }
            Integer port = body.getInteger("port");
            return new RegionNodeInfo(body.getString("id"), body.getString("host"), port, body.getString("table"));
        }
        int idx = raw.indexOf(':');
        if (idx < 0) {
            return new RegionNodeInfo(null, raw, null, null);
        }
        String id = raw.substring(0, idx).trim();
        String host = raw.substring(idx + 1).trim();
        return new RegionNodeInfo(id.isEmpty() ? null : id, host.isEmpty() ? null : host, null, null);
    }
}
