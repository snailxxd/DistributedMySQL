package com.example.master.service;

import com.example.master.balancer.LoadBalancer;
import com.example.master.cluster.RegionNode;
import com.example.master.cluster.RegionRegistry;
import com.example.master.meta.TableMetaStore;

/**
 * 客户端请求处理器：对应 Client 模块 MasterClient 的两类协议。
 *
 * <ul>
 *   <li>LOCATE_TABLE &lt;table&gt; → "REGION host:port" / "ERR ..."</li>
 *   <li>CREATE TABLE foo ...      → "OK CREATED foo ON host:port" / "ERR ..."</li>
 *   <li>DROP TABLE foo            → "OK DROPPED foo" / "ERR ..."</li>
 *   <li>其它 SQL                  → "ERR Master only accepts DDL"</li>
 * </ul>
 *
 * <p>所有响应均为单行，结尾不含换行（由网络层负责追加）。
 */
public final class MasterRequestHandler {

    private static final String CMD_LOCATE = "LOCATE_TABLE";

    private final TableMetaStore metaStore;
    private final RegionRegistry registry;
    private final LoadBalancer balancer;
    private final SqlClassifier sqlClassifier = new SqlClassifier();

    public MasterRequestHandler(TableMetaStore metaStore,
                                RegionRegistry registry,
                                LoadBalancer balancer) {
        this.metaStore = metaStore;
        this.registry = registry;
        this.balancer = balancer;
    }

    public String handle(String requestLine) {
        if (requestLine == null) {
            return "ERR empty request";
        }
        String line = requestLine.trim();
        if (line.isEmpty()) {
            return "ERR empty request";
        }

        if (line.regionMatches(true, 0, CMD_LOCATE, 0, CMD_LOCATE.length())) {
            return handleLocate(line);
        }
        return handleSql(line);
    }

    private String handleLocate(String line) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isEmpty()) {
            return "ERR LOCATE_TABLE requires table name";
        }
        String table = parts[1].trim();
        String regionId = metaStore.getRegionId(table);
        if (regionId == null) {
            return "ERR table not found: " + table;
        }
        RegionNode node = registry.get(regionId);
        if (node == null || node.getStatus() != RegionNode.Status.ACTIVE) {
            return "ERR region offline for table: " + table;
        }
        return "REGION " + node.toHostPort();
    }

    private String handleSql(String sql) {
        SqlClassifier.Result parsed = sqlClassifier.classify(sql);
        switch (parsed.kind) {
            case CREATE_TABLE:
                return handleCreate(parsed.table);
            case DROP_TABLE:
                return handleDrop(parsed.table);
            case OTHER:
            default:
                return "ERR Master only accepts DDL (CREATE TABLE / DROP TABLE)";
        }
    }

    private String handleCreate(String table) {
        if (table == null || table.isEmpty()) {
            return "ERR CREATE TABLE missing table name";
        }
        if (metaStore.exists(table)) {
            return "ERR table already exists: " + table;
        }
        RegionNode target = balancer.pick();
        if (target == null) {
            return "ERR no available RegionServer";
        }
        try {
            metaStore.addTable(table, target.getRegionId());
        } catch (Exception e) {
            return "ERR write meta failed: " + e.getMessage();
        }
        return "OK CREATED " + table + " ON " + target.toHostPort();
    }

    private String handleDrop(String table) {
        if (table == null || table.isEmpty()) {
            return "ERR DROP TABLE missing table name";
        }
        if (!metaStore.exists(table)) {
            return "ERR table not found: " + table;
        }
        try {
            metaStore.removeTable(table);
        } catch (Exception e) {
            return "ERR write meta failed: " + e.getMessage();
        }
        return "OK DROPPED " + table;
    }
}
