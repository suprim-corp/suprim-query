package dev.suprim.query.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleModelsTest {

    @Test
    void dbSort_render_shouldReturnSortClause() {
        DbSort sort = new DbSort("users", "u", "name", "ASC");

        String result = sort.render();

        assertThat(result).isEqualTo("u.name ASC ");
    }

    @Test
    void dbSort_recordAccessors_shouldReturnCorrectValues() {
        DbSort sort = new DbSort("orders", "o", "created_at", "DESC");

        assertThat(sort.table()).isEqualTo("orders");
        assertThat(sort.tableAlias()).isEqualTo("o");
        assertThat(sort.column()).isEqualTo("created_at");
        assertThat(sort.sortDirection()).isEqualTo("DESC");
    }

    @Test
    void dbMeta_recordAccessors_shouldReturnCorrectValues() {
        List<DbTable> tables = List.of();
        DbMeta meta = new DbMeta("PostgreSQL", 15, "PostgreSQL JDBC Driver", "42.7.0", tables);

        assertThat(meta.productName()).isEqualTo("PostgreSQL");
        assertThat(meta.majorVersion()).isEqualTo(15);
        assertThat(meta.driverName()).isEqualTo("PostgreSQL JDBC Driver");
        assertThat(meta.driverVersion()).isEqualTo("42.7.0");
        assertThat(meta.dbTables()).isEmpty();
    }

    @Test
    void dbWhere_isDelete_withDeleteOp_shouldReturnTrue() {
        DbWhere where = new DbWhere("users", null, List.of(), Map.of(), "delete", List.of());

        assertThat(where.isDelete()).isTrue();
    }

    @Test
    void dbWhere_isDelete_withDeleteUppercase_shouldReturnTrue() {
        DbWhere where = new DbWhere("users", null, List.of(), Map.of(), "DELETE", List.of());

        assertThat(where.isDelete()).isTrue();
    }

    @Test
    void dbWhere_isDelete_withSelectOp_shouldReturnFalse() {
        DbWhere where = new DbWhere("users", null, List.of(), Map.of(), "select", List.of());

        assertThat(where.isDelete()).isFalse();
    }

    @Test
    void dbWhere_recordAccessors_shouldReturnCorrectValues() {
        DbTable table = new DbTable("public", "users", "public.users", "u", List.of(), "TABLE", "\"");
        DbColumn col = new DbColumn("users", "id", "", "u", true, "bigint", false, false, Long.class, "\"", "");
        Map<String, Object> params = Map.of("id", 1L);
        List<DbTable> allTables = List.of(table);

        DbWhere where = new DbWhere("users", table, List.of(col), params, "update", allTables);

        assertThat(where.tableName()).isEqualTo("users");
        assertThat(where.table()).isEqualTo(table);
        assertThat(where.columns()).hasSize(1);
        assertThat(where.paramMap()).containsEntry("id", 1L);
        assertThat(where.op()).isEqualTo("update");
        assertThat(where.allTables()).hasSize(1);
    }

    @Test
    void dbAlias_recordAccessors_shouldReturnCorrectValues() {
        DbAlias alias = new DbAlias("data", "json_data", "->>'key'");

        assertThat(alias.name()).isEqualTo("data");
        assertThat(alias.alias()).isEqualTo("json_data");
        assertThat(alias.jsonParts()).isEqualTo("->>'key'");
    }

    @Test
    void metaDataTable_recordAccessors_shouldReturnCorrectValues() {
        MetaDataTable table = new MetaDataTable("users", "mydb", "public", "TABLE", "u_12345");

        assertThat(table.tableName()).isEqualTo("users");
        assertThat(table.catalog()).isEqualTo("mydb");
        assertThat(table.schema()).isEqualTo("public");
        assertThat(table.tableType()).isEqualTo("TABLE");
        assertThat(table.tableAlias()).isEqualTo("u_12345");
    }

    @Test
    void arrayTypeValueHolder_recordAccessors_shouldReturnCorrectValues() {
        Object[] values = {"a", "b", "c"};
        ArrayTypeValueHolder holder = new ArrayTypeValueHolder("VARCHAR", "varchar[]", values);

        assertThat(holder.jdbcType()).isEqualTo("VARCHAR");
        assertThat(holder.sqlType()).isEqualTo("varchar[]");
        assertThat(holder.values()).containsExactly("a", "b", "c");
    }

    @Test
    void columnLabel_shouldHaveExpectedValues() {
        assertThat(ColumnLabel.COLUMN_NAME.name()).isEqualTo("COLUMN_NAME");
        assertThat(ColumnLabel.DATA_TYPE.name()).isEqualTo("DATA_TYPE");
        assertThat(ColumnLabel.IS_AUTOINCREMENT.name()).isEqualTo("IS_AUTOINCREMENT");
        assertThat(ColumnLabel.TYPE_NAME.name()).isEqualTo("TYPE_NAME");
        assertThat(ColumnLabel.IS_GENERATED_COLUMN.name()).isEqualTo("IS_GENERATED_COLUMN");
        assertThat(ColumnLabel.TABLE_NAME.name()).isEqualTo("TABLE_NAME");
        assertThat(ColumnLabel.TABLE_CAT.name()).isEqualTo("TABLE_CAT");
        assertThat(ColumnLabel.TABLE_SCHEM.name()).isEqualTo("TABLE_SCHEM");
        assertThat(ColumnLabel.TABLE_TYPE.name()).isEqualTo("TABLE_TYPE");
    }

    @Test
    void databaseType_shouldHaveExpectedValues() {
        assertThat(DatabaseType.ORACLE.getName()).isEqualTo("Oracle");
        assertThat(DatabaseType.MSSQL.getName()).isEqualTo("Microsoft SQL Server");
        assertThat(DatabaseType.MYSQL.getName()).isEqualTo("MySQL");
        assertThat(DatabaseType.POSTGRESQL.getName()).isEqualTo("PostgreSQL");
        assertThat(DatabaseType.MARIADB.getName()).isEqualTo("MariaDB");
        assertThat(DatabaseType.SQLITE.getName()).isEqualTo("SQLite");
        assertThat(DatabaseType.DB2.getName()).isEqualTo("DB2/UDB");
    }

    @Test
    void databaseType_values_shouldContainAllTypes() {
        DatabaseType[] types = DatabaseType.values();
        assertThat(types).hasSize(7);
    }

    @Test
    void databaseType_fromString_withNull_shouldReturnEmpty() {
        assertThat(DatabaseType.fromString(null)).isEmpty();
    }

    @Test
    void databaseType_fromString_withEnumName_shouldMatch() {
        assertThat(DatabaseType.fromString("POSTGRESQL")).contains(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.fromString("postgresql")).contains(DatabaseType.POSTGRESQL);
    }

    @Test
    void databaseType_fromString_withDisplayName_shouldMatch() {
        assertThat(DatabaseType.fromString("PostgreSQL")).contains(DatabaseType.POSTGRESQL);
        assertThat(DatabaseType.fromString("Oracle")).contains(DatabaseType.ORACLE);
        assertThat(DatabaseType.fromString("Microsoft SQL Server")).contains(DatabaseType.MSSQL);
        assertThat(DatabaseType.fromString("MySQL")).contains(DatabaseType.MYSQL);
        assertThat(DatabaseType.fromString("MariaDB")).contains(DatabaseType.MARIADB);
        assertThat(DatabaseType.fromString("SQLite")).contains(DatabaseType.SQLITE);
        assertThat(DatabaseType.fromString("DB2/UDB")).contains(DatabaseType.DB2);
    }

    @Test
    void databaseType_fromString_withUnknown_shouldReturnEmpty() {
        assertThat(DatabaseType.fromString("UnknownDB")).isEmpty();
    }

    @Test
    void dbWhere_addParam_shouldAddToParamMap() {
        Map<String, Object> params = new HashMap<>();
        DbWhere where = new DbWhere("users", null, List.of(), params, "select", List.of());

        where.addParam("name", "John");

        assertThat(params).containsEntry("name", "John");
    }

    // --- UpsertConfig tests ---

    @Test
    void upsertConfig_validConfig_shouldCreateRecord() {
        UpsertConfig config = new UpsertConfig(List.of("email"), List.of("name", "age"));

        assertThat(config.conflictColumns()).containsExactly("email");
        assertThat(config.updateColumns()).containsExactly("name", "age");
        assertThat(config.isDoNothing()).isFalse();
    }

    @Test
    void upsertConfig_nullUpdateColumns_isDoNothingReturnsTrue() {
        UpsertConfig config = new UpsertConfig(List.of("email"), null);

        assertThat(config.isDoNothing()).isTrue();
    }

    @Test
    void upsertConfig_emptyUpdateColumns_isDoNothingReturnsTrue() {
        UpsertConfig config = new UpsertConfig(List.of("email"), List.of());

        assertThat(config.isDoNothing()).isTrue();
    }

    @Test
    void upsertConfig_nullConflictColumns_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new UpsertConfig(null, List.of("name")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflictColumns must not be null or empty");
    }

    @Test
    void upsertConfig_emptyConflictColumns_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new UpsertConfig(List.of(), List.of("name")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("conflictColumns must not be null or empty");
    }

    @Test
    void upsertConfig_multipleConflictColumns_shouldCreateRecord() {
        UpsertConfig config = new UpsertConfig(List.of("tenant_id", "email"), List.of("name"));

        assertThat(config.conflictColumns()).containsExactly("tenant_id", "email");
        assertThat(config.updateColumns()).containsExactly("name");
    }
}
