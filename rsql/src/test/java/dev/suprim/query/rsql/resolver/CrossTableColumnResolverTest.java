package dev.suprim.query.rsql.resolver;

import dev.suprim.query.exception.DbException;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for CrossTableColumnResolver.
 */
class CrossTableColumnResolverTest {

    private DbTable createTable(String name, String alias) {
        return new DbTable(
                "public",
                name,
                "\"public\".\"" + name + "\"",
                alias,
                List.of(
                        new DbColumn(name, "id", "", alias, false, "uuid", true, false, Object.class, "\"", null),
                        new DbColumn(name, "name", "", alias, false, "varchar", false, false, String.class, "\"", null),
                        new DbColumn(name, "color", "", alias, false, "varchar", false, false, String.class, "\"", null)
                ),
                "table",
                "\""
        );
    }

    @Test
    void resolveColumn_withSimpleColumnName_shouldUseFallbackTable() throws DbException {
        DbTable users = createTable("users", "u");
        List<DbTable> allTables = List.of(users);

        DbColumn result = CrossTableColumnResolver.resolveColumn("name", allTables, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }

    @Test
    void resolveColumn_withTablePrefix_shouldFindCorrectTable() throws DbException {
        DbTable tops = createTable("tops", "t");
        DbTable bottoms = createTable("bottoms", "b");
        List<DbTable> allTables = List.of(tops, bottoms);

        DbColumn result = CrossTableColumnResolver.resolveColumn("tops.color", allTables, tops);

        assertThat(result.name()).isEqualTo("color");
        assertThat(result.tableName()).isEqualTo("tops");
    }

    @Test
    void resolveColumn_withAliasPrefix_shouldFindCorrectTable() throws DbException {
        DbTable tops = createTable("tops", "t");
        DbTable bottoms = createTable("bottoms", "b");
        List<DbTable> allTables = List.of(tops, bottoms);

        DbColumn result = CrossTableColumnResolver.resolveColumn("b.color", allTables, tops);

        assertThat(result.name()).isEqualTo("color");
        assertThat(result.tableName()).isEqualTo("bottoms");
    }

    @Test
    void resolveColumn_withNonExistentTablePrefix_shouldFallbackToDefaultTable() throws DbException {
        DbTable users = createTable("users", "u");
        List<DbTable> allTables = List.of(users);

        // nonexistent.name - "nonexistent" not found as table, falls back to users
        // then tries to find "nonexistent.name" column in users - throws exception
        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("nonexistent.color", allTables, users))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column not found");
    }

    @Test
    void resolveColumn_withNullSelector_shouldThrowException() {
        DbTable users = createTable("users", "u");

        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn(null, List.of(users), users))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column selector cannot be blank");
    }

    @Test
    void resolveColumn_withBlankSelector_shouldThrowException() {
        DbTable users = createTable("users", "u");

        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("   ", List.of(users), users))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column selector cannot be blank");
    }

    @Test
    void resolveColumn_withEmptySelector_shouldThrowException() {
        DbTable users = createTable("users", "u");

        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("", List.of(users), users))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column selector cannot be blank");
    }

    @Test
    void resolveColumn_withNullFallbackTable_shouldThrowException() {
        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("name", List.of(), null))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("No fallback table available");
    }

    @Test
    void resolveColumn_withNullAllTables_shouldUseFallbackTable() throws DbException {
        DbTable users = createTable("users", "u");

        DbColumn result = CrossTableColumnResolver.resolveColumn("name", null, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }

    @Test
    void resolveColumn_withEmptyAllTables_shouldUseFallbackTable() throws DbException {
        DbTable users = createTable("users", "u");

        DbColumn result = CrossTableColumnResolver.resolveColumn("name", new ArrayList<>(), users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }

    @Test
    void resolveColumn_caseInsensitiveTableMatch_shouldWork() throws DbException {
        DbTable users = createTable("Users", "U");
        List<DbTable> allTables = List.of(users);

        DbColumn result = CrossTableColumnResolver.resolveColumn("users.name", allTables, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("Users");
    }

    @Test
    void resolveColumn_caseInsensitiveAliasMatch_shouldWork() throws DbException {
        DbTable users = createTable("users", "U");
        List<DbTable> allTables = List.of(users);

        DbColumn result = CrossTableColumnResolver.resolveColumn("u.name", allTables, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }

    @Test
    void resolveColumn_withDotInColumnName_shouldHandleCorrectly() throws DbException {
        DbTable users = createTable("users", "u");
        List<DbTable> allTables = List.of(users);

        // When prefix doesn't match any table, fallback table is used
        // Then buildColumn is called with "json.nested.key" as column name
        // This throws since such column doesn't exist in users table
        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("json.nested.key", allTables, users))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("Column not found");
    }

    @Test
    void resolveColumn_withNullTableAlias_shouldMatchOnlyByName() throws DbException {
        DbTable tableWithNullAlias = new DbTable(
                "public",
                "products",
                "\"public\".\"products\"",
                null,
                List.of(
                        new DbColumn("products", "id", "", null, false, "uuid", true, false, Object.class, "\"", null)
                ),
                "table",
                "\""
        );
        List<DbTable> allTables = List.of(tableWithNullAlias);

        DbColumn result = CrossTableColumnResolver.resolveColumn("products.id", allTables, tableWithNullAlias);

        assertThat(result.name()).isEqualTo("id");
        assertThat(result.tableName()).isEqualTo("products");
    }

    @Test
    void resolveColumn_withBlankAlias_shouldMatchOnlyByName() throws DbException {
        DbTable tableWithBlankAlias = new DbTable(
                "public",
                "products",
                "\"public\".\"products\"",
                "   ",
                List.of(
                        new DbColumn("products", "id", "", "   ", false, "uuid", true, false, Object.class, "\"", null)
                ),
                "table",
                "\""
        );
        List<DbTable> allTables = List.of(tableWithBlankAlias);

        DbColumn result = CrossTableColumnResolver.resolveColumn("products.id", allTables, tableWithBlankAlias);

        assertThat(result.name()).isEqualTo("id");
        assertThat(result.tableName()).isEqualTo("products");
    }
}
