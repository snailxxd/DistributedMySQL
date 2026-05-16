package com.example.regionserver.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple JDBC wrapper for executing SQL against local MySQL.
 */
public final class MySQLStorageEngine {

    public static final class ExecutionResult {
        public final boolean success;
        public final boolean isQuery;
        public final int rows;
        public final String message;

        private ExecutionResult(boolean success, boolean isQuery, int rows, String message) {
            this.success = success;
            this.isQuery = isQuery;
            this.rows = rows;
            this.message = message;
        }

        public static ExecutionResult okUpdate(int rows) {
            return new ExecutionResult(true, false, rows, "OK");
        }

        public static ExecutionResult okQuery(int rows) {
            return new ExecutionResult(true, true, rows, "OK");
        }

        public static ExecutionResult error(String msg) {
            return new ExecutionResult(false, false, 0, msg);
        }
    }

    private final String jdbcUrl;
    private final String jdbcUser;
    private final String jdbcPassword;

    public MySQLStorageEngine(String jdbcUrl, String jdbcUser, String jdbcPassword) {
        this.jdbcUrl = jdbcUrl;
        this.jdbcUser = jdbcUser;
        this.jdbcPassword = jdbcPassword;
    }

    public ExecutionResult execute(String sql) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword);
             Statement stmt = conn.createStatement()) {
            boolean isQuery = stmt.execute(sql);
            if (isQuery) {
                try (ResultSet rs = stmt.getResultSet()) {
                    int rows = 0;
                    while (rs.next()) {
                        rows++;
                    }
                    return ExecutionResult.okQuery(rows);
                }
            }
            int updated = stmt.getUpdateCount();
            return ExecutionResult.okUpdate(Math.max(updated, 0));
        } catch (Exception e) {
            return ExecutionResult.error(e.getMessage());
        }
    }

    public ExecutionResult clearTable(String physicalTable) {
        if (physicalTable == null || physicalTable.isEmpty()) {
            return ExecutionResult.error("missing table");
        }
        String sql = "TRUNCATE TABLE " + physicalTable;
        return execute(sql);
    }
}

