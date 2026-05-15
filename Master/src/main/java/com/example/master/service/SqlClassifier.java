package com.example.master.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极简 SQL 分类器：只识别 CREATE TABLE / DROP TABLE 并抽出表名，
 * 其余一律返回 OTHER（Master 拒绝处理）。
 *
 * <p>该实现刻意不依赖 Client 模块，保持 Master 的独立性，
 * 但与 Client 模块的 SqlParser 行为一致。
 */
public final class SqlClassifier {

    public enum Kind {
        CREATE_TABLE,
        DROP_TABLE,
        OTHER
    }

    private static final Pattern CREATE_TABLE =
            Pattern.compile("(?i)^CREATE\\s+TABLE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");
    private static final Pattern DROP_TABLE =
            Pattern.compile("(?i)^DROP\\s+TABLE\\s+([`\"\\[]?[\\w.]+[`\"\\]]?)");

    public static final class Result {
        public final Kind kind;
        public final String table;

        private Result(Kind kind, String table) {
            this.kind = kind;
            this.table = table;
        }
    }

    public Result classify(String sql) {
        String normalized = normalize(sql);
        Matcher m = CREATE_TABLE.matcher(normalized);
        if (m.find()) {
            return new Result(Kind.CREATE_TABLE, strip(m.group(1)));
        }
        m = DROP_TABLE.matcher(normalized);
        if (m.find()) {
            return new Result(Kind.DROP_TABLE, strip(m.group(1)));
        }
        return new Result(Kind.OTHER, null);
    }

    private String normalize(String sql) {
        String s = sql == null ? "" : sql.trim();
        if (s.endsWith(";")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    private String strip(String table) {
        if (table == null) {
            return null;
        }
        String t = table.trim();
        if ((t.startsWith("`") && t.endsWith("`"))
                || (t.startsWith("\"") && t.endsWith("\""))
                || (t.startsWith("[") && t.endsWith("]"))) {
            return t.substring(1, t.length() - 1);
        }
        return t;
    }
}
