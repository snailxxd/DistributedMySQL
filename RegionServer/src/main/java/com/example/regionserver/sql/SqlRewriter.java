package com.example.regionserver.sql;

import java.util.regex.Pattern;

/**
 * Rewrites logical table names to physical table names for local storage.
 */
public final class SqlRewriter {

    private SqlRewriter() {
    }

    public static String rewrite(String sql, String logicalTable, String physicalTable) {
        if (sql == null || logicalTable == null || physicalTable == null) {
            return sql;
        }
        String escaped = Pattern.quote(logicalTable);
        String rewritten = sql;
        rewritten = rewritten.replaceAll("(?i)\\bFROM\\s+" + escaped + "\\b", "FROM " + physicalTable);
        rewritten = rewritten.replaceAll("(?i)\\bINTO\\s+" + escaped + "\\b", "INTO " + physicalTable);
        rewritten = rewritten.replaceAll("(?i)^UPDATE\\s+" + escaped + "\\b", "UPDATE " + physicalTable);
        rewritten = rewritten.replaceAll("(?i)^DELETE\\s+FROM\\s+" + escaped + "\\b", "DELETE FROM " + physicalTable);
        return rewritten;
    }
}

