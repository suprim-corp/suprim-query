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
    void constructor_shouldBeInstantiable() {
        // Cover the implicit default constructor (line 20)
        CrossTableColumnResolver resolver = new CrossTableColumnResolver();
        assertThat(resolver).isNotNull();
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

    @Test
    void resolveColumn_withDotPrefix_andNullAllTables_shouldFallback() throws DbException {
        // This exercises findTableByPrefix with null allTables (line 109 null branch)
        DbTable users = createTable("users", "u");

        // "foo.id" has a dot, so it enters the dot-prefix path, calls findTableByPrefix(null)
        // findTableByPrefix returns null → falls through to fallback
        // fallback tries buildColumn("foo.id") which won't exist → but let's use a column that does
        // Actually "foo" won't match any table, so it falls through to fallback with selector "foo.id"
        // which won't be found in users table
        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("foo.id", null, users))
                .isInstanceOf(DbException.class);
    }

    @Test
    void resolveColumn_withDotPrefix_andEmptyAllTables_shouldFallback() throws DbException {
        // This exercises findTableByPrefix with empty allTables (line 109 isEmpty branch)
        DbTable users = createTable("users", "u");

        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("foo.id", new ArrayList<>(), users))
                .isInstanceOf(DbException.class);
    }

    @Test
    void resolveColumn_withDotPrefix_nullAllTables_nullFallback_shouldThrow() {
        // dot-prefix path → findTableByPrefix(null) → returns null → fallback is null → throws
        assertThatThrownBy(() -> CrossTableColumnResolver.resolveColumn("foo.id", null, null))
                .isInstanceOf(DbException.class)
                .hasMessageContaining("No fallback table available");
    }

    @Test
    void resolveColumn_withDotPrefix_aliasDoesNotMatch_shouldContinueLoop() throws DbException {
        // Table has a valid alias "x" but prefix is "y" which doesn't match name or alias of first table.
        // This exercises the non-matching alias branch (nonNull && !isBlank && !equalsIgnoreCase) on first iteration.
        DbTable tops = new DbTable(
                "public",
                "tops",
                "\"public\".\"tops\"",
                "x",
                List.of(
                        new DbColumn("tops", "color", "", "x", false, "varchar", false, false, String.class, "\"", null)
                ),
                "table",
                "\""
        );
        DbTable bottoms = new DbTable(
                "public",
                "bottoms",
                "\"public\".\"bottoms\"",
                "y",
                List.of(
                        new DbColumn("bottoms", "color", "", "y", false, "varchar", false, false, String.class, "\"", null)
                ),
                "table",
                "\""
        );
        List<DbTable> allTables = List.of(tops, bottoms);

        // prefix "y" doesn't match tops.name ("tops") nor tops.alias ("x"),
        // but matches bottoms.alias ("y") — exercises the non-matching alias branch on first iteration
        DbColumn result = CrossTableColumnResolver.resolveColumn("y.color", allTables, tops);

        assertThat(result.name()).isEqualTo("color");
        assertThat(result.tableName()).isEqualTo("bottoms");
    }

    @Test
    void resolveColumn_withDotPrefix_tableHasNullAlias_prefixDoesNotMatchName_shouldSkip() throws DbException {
        // Table with null alias where prefix doesn't match name → exercises nonNull(alias) == false branch at line 127
        DbTable tableNullAlias = new DbTable(
                "public",
                "orders",
                "\"public\".\"orders\"",
                null,
                List.of(
                        new DbColumn("orders", "id", "", null, false, "uuid", true, false, Object.class, "\"", null)
                ),
                "table",
                "\""
        );
        DbTable users = createTable("users", "u");
        List<DbTable> allTables = List.of(tableNullAlias, users);

        // prefix "u" doesn't match orders.name, orders.alias is null → skips alias check
        // then matches users by alias "u"
        DbColumn result = CrossTableColumnResolver.resolveColumn("u.name", allTables, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }

    @Test
    void resolveColumn_withDotPrefix_tableHasBlankAlias_prefixDoesNotMatchName_shouldSkip() throws DbException {
        // Table with blank alias where prefix doesn't match name → exercises !alias.isBlank() == false branch at line 127
        DbTable tableBlankAlias = new DbTable(
                "public",
                "orders",
                "\"public\".\"orders\"",
                "  ",
                List.of(
                        new DbColumn("orders", "id", "", "  ", false, "uuid", true, false, Object.class, "\"", null)
                ),
                "table",
                "\""
        );
        DbTable users = createTable("users", "u");
        List<DbTable> allTables = List.of(tableBlankAlias, users);

        // prefix "u" doesn't match orders.name, orders.alias is blank → skips alias check
        // then matches users by alias "u"
        DbColumn result = CrossTableColumnResolver.resolveColumn("u.name", allTables, users);

        assertThat(result.name()).isEqualTo("name");
        assertThat(result.tableName()).isEqualTo("users");
    }
}
