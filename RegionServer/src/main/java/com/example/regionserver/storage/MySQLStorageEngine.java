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
        public final String payload;

        private ExecutionResult(boolean success, boolean isQuery, int rows, String message, String payload) {
            this.success = success;
            this.isQuery = isQuery;
            this.rows = rows;
            this.message = message;
            this.payload = payload;
        }

        public static ExecutionResult okUpdate(int rows) {
            return new ExecutionResult(true, false, rows, "OK", null);
        }

        public static ExecutionResult okQuery(int rows, String payload) {
            return new ExecutionResult(true, true, rows, "OK", payload);
        }

        public static ExecutionResult error(String msg) {
            return new ExecutionResult(false, false, 0, msg, null);
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
                    java.sql.ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    StringBuilder table = new StringBuilder();
                    for (int i = 1; i <= colCount; i++) {
                        if (i > 1) {
                            table.append(" | ");
                        }
                        table.append(meta.getColumnLabel(i));
                    }
                    int rowCount = 0;
                    while (rs.next()) {
                        table.append("\n");
                        for (int i = 1; i <= colCount; i++) {
                            if (i > 1) {
                                table.append(" | ");
                            }
                            Object value = rs.getObject(i);
                            table.append(value == null ? "NULL" : value.toString());
                        }
                        rowCount++;
                    }
                    table.append("\nRows: ").append(rowCount);
                    return ExecutionResult.okQuery(rowCount, table.toString());
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
