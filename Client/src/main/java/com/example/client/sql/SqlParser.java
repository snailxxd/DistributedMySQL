package com.example.client.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlParser {

    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile("(?i)^CREATE\\s+TABLE\\b.*");
    private static final Pattern DROP_TABLE_PATTERN = Pattern.compile("(?i)^DROP\\s+TABLE\\b.*");
    private static final Pattern CREATE_TABLE_NAME_PATTERN = Pattern.compile("(?i)^CREATE\\s+TABLE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern DROP_TABLE_NAME_PATTERN = Pattern.compile("(?i)^DROP\\s+TABLE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile("(?i)\\bFROM\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern INTO_TABLE_PATTERN = Pattern.compile("(?i)\\bINTO\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("(?i)^UPDATE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile("(?i)^DELETE\\s+FROM\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");

    /**
     * 解析 SQL，提取路由所需的最小信息。
     */
    public ParseResult parse(String sql) {
        String normalized = normalize(sql);
        if (normalized.isEmpty()) {
            return new ParseResult("", false, false, null);
        }
        boolean isCreate = CREATE_TABLE_PATTERN.matcher(normalized).matches();
        boolean isDrop = DROP_TABLE_PATTERN.matcher(normalized).matches();
        String table = extractTable(normalized);
        return new ParseResult(normalized, isCreate, isDrop, table);
    }

    /**
     * 统一 SQL 格式，去掉首尾空格与结尾分号。
     */
    private String normalize(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    /**
     * 从 SQL 中提取表名。
     */
    private String extractTable(String sql) {
        String table = matchGroup(CREATE_TABLE_NAME_PATTERN, sql);
        if (table != null) {
            return table;
        }
        table = matchGroup(DROP_TABLE_NAME_PATTERN, sql);
        if (table != null) {
            return table;
        }
        table = matchGroup(UPDATE_TABLE_PATTERN, sql);
        if (table != null) {
            return table;
        }
        table = matchGroup(DELETE_TABLE_PATTERN, sql);
        if (table != null) {
            return table;
        }
        table = matchGroup(INTO_TABLE_PATTERN, sql);
        if (table != null) {
            return table;
        }
        return matchGroup(FROM_TABLE_PATTERN, sql);
    }

    /**
     * 通用正则匹配，返回第一个捕获组。
     */
    private String matchGroup(Pattern pattern, String sql) {
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return stripTableQuotes(matcher.group(1));
        }
        return null;
    }

    /**
     * 去掉表名外侧的反引号/引号/方括号。
     */
    private String stripTableQuotes(String table) {
        if (table == null) {
            return null;
        }
        String trimmed = table.trim();
        if ((trimmed.startsWith("`") && trimmed.endsWith("`"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    public static class ParseResult {
        private final String normalizedSql;
        private final boolean createTable;
        private final boolean dropTable;
        private final String table;

        private ParseResult(String normalizedSql, boolean createTable, boolean dropTable, String table) {
            this.normalizedSql = normalizedSql;
            this.createTable = createTable;
            this.dropTable = dropTable;
            this.table = table;
        }

        public String getNormalizedSql() {
            return normalizedSql;
        }

        public boolean isCreateOrDrop() {
            return createTable || dropTable;
        }

        public String getTable() {
            return table;
        }

    }
}
