package com.example.regionserver.sql;

import org.testng.annotations.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SqlRoutingParserTest {

    @Test
    void parsesInsertKeyByColumn() {
        SqlRoutingParser parser = new SqlRoutingParser();
        SqlRoutingParser.ParseResult result = parser.parse(
                "INSERT INTO User (name, id) VALUES ('a', 42)");
        assertEquals("User", result.getTable());
        assertEquals(42L, result.getKey());
    }

    @Test
    void parsesWhereId() {
        SqlRoutingParser parser = new SqlRoutingParser();
        SqlRoutingParser.ParseResult result = parser.parse(
                "SELECT * FROM user WHERE id = 7");
        assertEquals("user", result.getTable());
        assertEquals(7L, result.getKey());
    }

    @Test
    void missingKeyReturnsNull() {
        SqlRoutingParser parser = new SqlRoutingParser();
        SqlRoutingParser.ParseResult result = parser.parse(
                "SELECT * FROM user WHERE name = 'x'");
        assertEquals("user", result.getTable());
        assertNull(result.getKey());
    }
}

