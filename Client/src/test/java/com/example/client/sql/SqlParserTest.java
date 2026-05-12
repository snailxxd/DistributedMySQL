package com.example.client.sql;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlParserTest {

    private final SqlParser parser = new SqlParser();

    @Test
    void parseCreateDrop() {
        SqlParser.ParseResult create = parser.parse("CREATE TABLE users (id int)");
        assertTrue(create.isCreateOrDrop());
        assertEquals("users", create.getTable());

        SqlParser.ParseResult drop = parser.parse("DROP TABLE users");
        assertTrue(drop.isCreateOrDrop());
        assertEquals("users", drop.getTable());
    }

    @Test
    void parseSelectInsertUpdateDelete() {
        SqlParser.ParseResult select = parser.parse("SELECT * FROM user WHERE id = 1;");
        assertFalse(select.isCreateOrDrop());
        assertEquals("user", select.getTable());

        SqlParser.ParseResult insert = parser.parse("INSERT INTO user (id,name) VALUES (1,'a')");
        assertEquals("user", insert.getTable());

        SqlParser.ParseResult update = parser.parse("UPDATE user SET name='b' WHERE id=1");
        assertEquals("user", update.getTable());

        SqlParser.ParseResult delete = parser.parse("DELETE FROM user WHERE id=1");
        assertEquals("user", delete.getTable());
    }

    @Test
    void parseQuotedAndQualifiedTable() {
        SqlParser.ParseResult quoted = parser.parse("SELECT * FROM `db.table` WHERE id=1");
        assertEquals("db.table", quoted.getTable());

        SqlParser.ParseResult quoted2 = parser.parse("SELECT * FROM \"db.table\" WHERE id=1");
        assertEquals("db.table", quoted2.getTable());

        SqlParser.ParseResult bracketed = parser.parse("SELECT * FROM [db.table] WHERE id=1");
        assertEquals("db.table", bracketed.getTable());
    }

    @Test
    void parseEmptyOrNull() {
        SqlParser.ParseResult empty = parser.parse("   ");
        assertEquals("", empty.getNormalizedSql());
        assertNull(empty.getTable());

        SqlParser.ParseResult nul = parser.parse(null);
        assertEquals("", nul.getNormalizedSql());
        assertNull(nul.getTable());
    }
}

