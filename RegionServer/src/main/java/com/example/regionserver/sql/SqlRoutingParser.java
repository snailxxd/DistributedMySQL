package com.example.regionserver.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts table and key info from SQL for routing.
 */
public final class SqlRoutingParser {

    private static final Pattern FROM_TABLE_PATTERN = Pattern.compile("(?i)\\bFROM\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern INTO_TABLE_PATTERN = Pattern.compile("(?i)\\bINTO\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern UPDATE_TABLE_PATTERN = Pattern.compile("(?i)^UPDATE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern DELETE_TABLE_PATTERN = Pattern.compile("(?i)^DELETE\\s+FROM\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");

    private static final Pattern WHERE_EQ_PATTERN = Pattern.compile(
            "(?i)\\bWHERE\\s+([`\"\\[]?\\w+[`\"\\]]?)\\s*=\\s*([^\\s;]+)");

    private static final Pattern INSERT_COLUMNS_PATTERN = Pattern.compile(
            "(?i)^INSERT\\s+INTO\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)\\s*\\(([^)]+)\\)\\s*VALUES\\s*\\(([^)]+)\\)");
    private static final Pattern INSERT_VALUES_PATTERN = Pattern.compile(
            "(?i)^INSERT\\s+INTO\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)\\s*VALUES\\s*\\(([^)]+)\\)");

    public ParseResult parse(String sql) {
        String normalized = normalize(sql);
        if (normalized.isEmpty()) {
            return new ParseResult("", null, null);
        }
        String table = extractTable(normalized);
        Long key = extractKey(normalized);
        return new ParseResult(normalized, table, key);
    }

    private String normalize(String sql) {
        String normalized = sql == null ? "" : sql.trim();
        if (normalized.endsWith(";")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String extractTable(String sql) {
        String table = matchGroup(UPDATE_TABLE_PATTERN, sql);
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

    private Long extractKey(String sql) {
        Matcher insertWithColumns = INSERT_COLUMNS_PATTERN.matcher(sql);
        if (insertWithColumns.find()) {
            String columns = insertWithColumns.group(2);
            String values = insertWithColumns.group(3);
            return extractKeyFromInsert(columns, values);
        }
        Matcher insertSimple = INSERT_VALUES_PATTERN.matcher(sql);
        if (insertSimple.find()) {
            String values = insertSimple.group(2);
            String first = splitCsv(values)[0];
            return parseLongValue(first);
        }
        Matcher where = WHERE_EQ_PATTERN.matcher(sql);
        if (where.find()) {
            String col = stripQuotes(where.group(1));
            if ("id".equalsIgnoreCase(col)) {
                return parseLongValue(where.group(2));
            }
        }
        return null;
    }

    private Long extractKeyFromInsert(String columns, String values) {
        String[] cols = splitCsv(columns);
        String[] vals = splitCsv(values);
        int limit = Math.min(cols.length, vals.length);
        for (int i = 0; i < limit; i++) {
            if ("id".equalsIgnoreCase(stripQuotes(cols[i]))) {
                return parseLongValue(vals[i]);
            }
        }
        return null;
    }

    private String[] splitCsv(String csv) {
        return csv.split("\\s*,\\s*");
    }

    private Long parseLongValue(String raw) {
        String cleaned = raw == null ? "" : raw.trim();
        if (cleaned.startsWith("'")) {
            cleaned = cleaned.substring(1);
        }
        if (cleaned.endsWith("'")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        try {
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String matchGroup(Pattern pattern, String sql) {
        Matcher matcher = pattern.matcher(sql);
        if (matcher.find()) {
            return stripQuotes(matcher.group(1));
        }
        return null;
    }

    private String stripQuotes(String table) {
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

    public static final class ParseResult {
        private final String normalizedSql;
        private final String table;
        private final Long key;

        private ParseResult(String normalizedSql, String table, Long key) {
            this.normalizedSql = normalizedSql;
            this.table = table;
            this.key = key;
        }

        public String getNormalizedSql() {
            return normalizedSql;
        }

        public String getTable() {
            return table;
        }

        public Long getKey() {
            return key;
        }
    }
}

