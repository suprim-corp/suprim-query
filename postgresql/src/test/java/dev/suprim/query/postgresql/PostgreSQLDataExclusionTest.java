package dev.suprim.query.postgresql;

import dev.suprim.query.model.ColumnLabel;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.MetaDataTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostgreSQLDataExclusion — no Docker/Testcontainers required.
 */
class PostgreSQLDataExclusionTest {

    private PostgreSQLDataExclusion dataExclusion;

    @BeforeEach
    void setUp() {
        dataExclusion = new PostgreSQLDataExclusion();
    }

    // ==================== canHandle ====================

    @Test
    void canHandle_postgresql_shouldReturnTrue() {
        assertThat(dataExclusion.canHandle("PostgreSQL")).isTrue();
        assertThat(dataExclusion.canHandle("postgresql")).isTrue();
        assertThat(dataExclusion.canHandle("POSTGRESQL")).isTrue();
    }

    @Test
    void canHandle_otherDatabase_shouldReturnFalse() {
        assertThat(dataExclusion.canHandle("MySQL")).isFalse();
        assertThat(dataExclusion.canHandle("Oracle")).isFalse();
        assertThat(dataExclusion.canHandle("SQLServer")).isFalse();
    }

    // ==================== getTables — includeAllSchemas ====================

    @Test
    void getTables_includeAllSchemas_shouldExcludeSystemSchemas() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        // Mock getSchemas() to return pg_catalog, information_schema, public, app
        ResultSet schemasRs = mockResultSet(
                List.of("pg_catalog", "information_schema", "public", "app"),
                ColumnLabel.TABLE_SCHEM.name()
        );
        when(metaData.getSchemas()).thenReturn(schemasRs);

        // Mock getTables for "public" schema
        ResultSet publicTablesRs = mockTableResultSet(
                tableList(new String[]{"users", null, "public", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(publicTablesRs);

        // Mock getTables for "app" schema
        ResultSet appTablesRs = mockTableResultSet(
                tableList(new String[]{"orders", null, "app", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("app"), isNull(), any(String[].class)))
                .thenReturn(appTablesRs);

        // Mock getPrimaryKeys and getColumns for each table
        mockColumnsAndPks(metaData, null, "public", "users");
        mockColumnsAndPks(metaData, null, "app", "orders");

        List<DbTable> tables = dataExclusion.getTables(metaData, true, null);

        assertThat(tables).hasSize(2);
        List<String> tableNames = tables.stream().map(DbTable::name).toList();
        assertThat(tableNames).containsExactlyInAnyOrder("users", "orders");
    }

    // ==================== getTables — specific schemas ====================

    @Test
    void getTables_includeSpecificSchema_shouldReturnOnlyFromThatSchema() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet publicTablesRs = mockTableResultSet(
                tableList(new String[]{"users", null, "public", "TABLE"},
                        new String[]{"orders", null, "public", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(publicTablesRs);

        mockColumnsAndPks(metaData, null, "public", "users");
        mockColumnsAndPks(metaData, null, "public", "orders");

        List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

        assertThat(tables).hasSize(2);
        List<String> tableNames = tables.stream().map(DbTable::name).toList();
        assertThat(tableNames).containsExactlyInAnyOrder("users", "orders");
    }

    // ==================== getTables — empty schema ====================

    @Test
    void getTables_emptySchema_shouldReturnEmptyList() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet emptyRs = mock(ResultSet.class);
        when(emptyRs.next()).thenReturn(false);
        when(metaData.getTables(isNull(), eq("empty_schema"), isNull(), any(String[].class)))
                .thenReturn(emptyRs);

        List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("empty_schema"));

        assertThat(tables).isEmpty();
    }

    // ==================== getTables — exception ====================

    @Test
    void getTables_withDatabaseMetaDataException_shouldThrowRuntimeException() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getSchemas()).thenThrow(new SQLException("Connection error"));

        assertThatThrownBy(() -> dataExclusion.getTables(metaData, true, null))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== getTables — column metadata ====================

    @Test
    void getTables_shouldPopulateColumnsCorrectly() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet tablesRs = mockTableResultSet(
                tableList(new String[]{"users", "testdb", "public", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(tablesRs);

        // Mock primary keys — "id" is PK
        ResultSet pkRs = mock(ResultSet.class);
        when(pkRs.next()).thenReturn(true, false);
        when(pkRs.getString(ColumnLabel.COLUMN_NAME.name())).thenReturn("id");
        when(metaData.getPrimaryKeys(eq("testdb"), eq("public"), eq("users"))).thenReturn(pkRs);

        // Mock columns
        ResultSet colsRs = mock(ResultSet.class);
        when(colsRs.next()).thenReturn(true, true, false);
        when(colsRs.getString(ColumnLabel.COLUMN_NAME.name())).thenReturn("id", "name");
        when(colsRs.getInt(ColumnLabel.DATA_TYPE.name())).thenReturn(4, 12); // INTEGER, VARCHAR
        when(colsRs.getString(ColumnLabel.IS_AUTOINCREMENT.name())).thenReturn("YES", "NO");
        when(colsRs.getString(ColumnLabel.TYPE_NAME.name())).thenReturn("int4", "varchar");
        when(metaData.getColumns(eq("testdb"), eq("public"), eq("users"), isNull())).thenReturn(colsRs);

        List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

        assertThat(tables).hasSize(1);
        DbTable usersTable = tables.getFirst();
        assertThat(usersTable.name()).isEqualTo("users");
        assertThat(usersTable.schema()).isEqualTo("public");
        assertThat(usersTable.fullName()).isEqualTo("public.users");
        assertThat(usersTable.coverChar()).isEqualTo("\"");
        assertThat(usersTable.dbColumns()).hasSize(2);

        // Check PK detection
        DbColumn idCol = usersTable.dbColumns().stream()
                .filter(c -> c.name().equals("id")).findFirst().orElseThrow();
        assertThat(idCol.pk()).isTrue();
        assertThat(idCol.autoIncremented()).isTrue();

        DbColumn nameCol = usersTable.dbColumns().stream()
                .filter(c -> c.name().equals("name")).findFirst().orElseThrow();
        assertThat(nameCol.pk()).isFalse();
        assertThat(nameCol.autoIncremented()).isFalse();
    }

    // ==================== getDbTable — null/blank schema fallback to catalog ====================

    @Test
    void getTables_withNullSchema_shouldUseCatalogAsSchemaName() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet tablesRs = mockTableResultSet(
                tableList(new String[]{"users", "mydb", null, "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(tablesRs);

        mockColumnsAndPks(metaData, "mydb", null, "users");

        List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

        assertThat(tables).hasSize(1);
        // When schema is null, catalog ("mydb") should be used
        assertThat(tables.getFirst().schema()).isEqualTo("mydb");
    }

    @Test
    void getTables_withBlankSchema_shouldUseCatalogAsSchemaName() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet tablesRs = mockTableResultSet(
                tableList(new String[]{"users", "mydb", "   ", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(tablesRs);

        mockColumnsAndPks(metaData, "mydb", "   ", "users");

        List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

        assertThat(tables).hasSize(1);
        assertThat(tables.getFirst().schema()).isEqualTo("mydb");
    }

    // ==================== getDbTable — SQLException wrapping ====================

    @Test
    void getTables_withColumnFetchException_shouldThrowRuntimeException() throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        ResultSet tablesRs = mockTableResultSet(
                tableList(new String[]{"users", "testdb", "public", "TABLE"})
        );
        when(metaData.getTables(isNull(), eq("public"), isNull(), any(String[].class)))
                .thenReturn(tablesRs);

        when(metaData.getPrimaryKeys(any(), any(), any()))
                .thenThrow(new SQLException("Primary key error"));

        assertThatThrownBy(() -> dataExclusion.getTables(metaData, false, List.of("public")))
                .isInstanceOf(RuntimeException.class);
    }

    // ==================== helpers ====================

    private static List<String[]> tableList(String[]... entries) {
        return Arrays.asList(entries);
    }

    private ResultSet mockResultSet(List<String> values, String columnName) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Boolean[] nexts = new Boolean[values.size() + 1];
        for (int i = 0; i < values.size(); i++) nexts[i] = true;
        nexts[values.size()] = false;
        when(rs.next()).thenReturn(nexts[0], java.util.Arrays.copyOfRange(nexts, 1, nexts.length));
        when(rs.getString(columnName)).thenReturn(
                values.getFirst(),
                values.subList(1, values.size()).toArray(new String[0])
        );
        return rs;
    }

    @SuppressWarnings("unchecked")
    private ResultSet mockTableResultSet(List<String[]> tables) throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        Boolean[] nexts = new Boolean[tables.size() + 1];
        for (int i = 0; i < tables.size(); i++) nexts[i] = true;
        nexts[tables.size()] = false;
        when(rs.next()).thenReturn(nexts[0], java.util.Arrays.copyOfRange(nexts, 1, nexts.length));

        String[] tableNames = tables.stream().map(t -> t[0]).toArray(String[]::new);
        String[] catalogs = tables.stream().map(t -> t[1]).toArray(String[]::new);
        String[] schemas = tables.stream().map(t -> t[2]).toArray(String[]::new);
        String[] types = tables.stream().map(t -> t[3]).toArray(String[]::new);

        when(rs.getString(ColumnLabel.TABLE_NAME.name()))
                .thenReturn(tableNames[0], java.util.Arrays.copyOfRange(tableNames, 1, tableNames.length));
        when(rs.getString(ColumnLabel.TABLE_CAT.name()))
                .thenReturn(catalogs[0], java.util.Arrays.copyOfRange(catalogs, 1, catalogs.length));
        when(rs.getString(ColumnLabel.TABLE_SCHEM.name()))
                .thenReturn(schemas[0], java.util.Arrays.copyOfRange(schemas, 1, schemas.length));
        when(rs.getString(ColumnLabel.TABLE_TYPE.name()))
                .thenReturn(types[0], java.util.Arrays.copyOfRange(types, 1, types.length));

        return rs;
    }

    private void mockColumnsAndPks(DatabaseMetaData metaData, String catalog, String schema, String tableName)
            throws SQLException {
        // Empty PK result
        ResultSet pkRs = mock(ResultSet.class);
        when(pkRs.next()).thenReturn(false);
        when(metaData.getPrimaryKeys(eq(catalog), eq(schema), eq(tableName))).thenReturn(pkRs);

        // Single column "id"
        ResultSet colsRs = mock(ResultSet.class);
        when(colsRs.next()).thenReturn(true, false);
        when(colsRs.getString(ColumnLabel.COLUMN_NAME.name())).thenReturn("id");
        when(colsRs.getInt(ColumnLabel.DATA_TYPE.name())).thenReturn(4); // INTEGER
        when(colsRs.getString(ColumnLabel.IS_AUTOINCREMENT.name())).thenReturn("NO");
        when(colsRs.getString(ColumnLabel.TYPE_NAME.name())).thenReturn("int4");
        when(metaData.getColumns(eq(catalog), eq(schema), eq(tableName), isNull())).thenReturn(colsRs);
    }
}
