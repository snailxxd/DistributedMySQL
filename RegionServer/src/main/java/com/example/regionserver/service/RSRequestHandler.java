package com.example.regionserver.service;

import com.example.regionserver.region.RegionManager;
import com.example.regionserver.sql.SqlRewriter;
import com.example.regionserver.sql.SqlRoutingParser;
import com.example.regionserver.storage.MySQLStorageEngine;

/**
 * Handles client requests coming to the RegionServer socket.
 */
public final class RSRequestHandler {

    private final RegionManager regionManager;
    private final MySQLStorageEngine storageEngine;
    private final SqlRoutingParser routingParser;

    public RSRequestHandler(RegionManager regionManager,
                            MySQLStorageEngine storageEngine,
                            SqlRoutingParser routingParser) {
        this.regionManager = regionManager;
        this.storageEngine = storageEngine;
        this.routingParser = routingParser;
    }

    public String handle(String requestLine) {
        if (requestLine == null) {
            return "ERR empty request";
        }
        String line = requestLine.trim();
        if (line.isEmpty()) {
            return "ERR empty request";
        }
        if ("PING".equalsIgnoreCase(line)) {
            return "OK PONG";
        }

        SqlRoutingParser.ParseResult parsed = routingParser.parse(line);
        if (parsed.getNormalizedSql().isEmpty()) {
            return "ERR empty sql";
        }

        RegionManager.RouteResult route = regionManager.route(parsed.getTable(), parsed.getKey());
        if (route.status == RegionManager.RouteStatus.MOVED) {
            return "MOVED " + route.message;
        }
        if (route.status == RegionManager.RouteStatus.ERROR) {
            return route.message;
        }

        String rewritten = SqlRewriter.rewrite(
                parsed.getNormalizedSql(),
                regionManager.getLogicalTable(),
                regionManager.getPhysicalTable());

        MySQLStorageEngine.ExecutionResult result = storageEngine.execute(rewritten);
        if (!result.success) {
            return "ERR " + result.message;
        }
        if (result.isQuery) {
            return "OK " + result.rows + " rows";
        }
        return "OK " + result.rows + " affected";
    }
}

