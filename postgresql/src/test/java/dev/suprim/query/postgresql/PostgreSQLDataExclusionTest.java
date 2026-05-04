package dev.suprim.query.postgresql;

import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.MetaDataTable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Method;
import java.sql.*;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for PostgreSQLDataExclusion.
 */
@Testcontainers
class PostgreSQLDataExclusionTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private PostgreSQLDataExclusion dataExclusion;

    @BeforeAll
    static void setUpDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            // Create test schema and tables
            stmt.execute("CREATE SCHEMA IF NOT EXISTS test_schema");

            stmt.execute("""
                CREATE TABLE public.users (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE public.orders (
                    id SERIAL PRIMARY KEY,
                    user_id INT REFERENCES public.users(id),
                    total NUMERIC(10,2),
                    status VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE test_schema.products (
                    id SERIAL PRIMARY KEY,
                    name VARCHAR(200),
                    price NUMERIC(10,2)
                )
            """);
        }
    }

    @BeforeEach
    void setUp() {
        dataExclusion = new PostgreSQLDataExclusion();
    }

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

    @Test
    void getTables_includeAllSchemas_shouldReturnTablesFromAllSchemas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, true, null);

            assertThat(tables).isNotEmpty();
            // Should contain tables from public and test_schema, but not pg_catalog or information_schema
            List<String> tableNames = tables.stream().map(DbTable::name).toList();
            assertThat(tableNames).contains("users", "orders", "products");
        }
    }

    @Test
    void getTables_includeSpecificSchema_shouldReturnOnlyFromThatSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

            assertThat(tables).isNotEmpty();
            List<String> tableNames = tables.stream().map(DbTable::name).toList();
            assertThat(tableNames).contains("users", "orders");
            assertThat(tableNames).doesNotContain("products");
        }
    }

    @Test
    void getTables_includeMultipleSchemas_shouldReturnFromAllSpecified() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public", "test_schema"));

            assertThat(tables).isNotEmpty();
            List<String> tableNames = tables.stream().map(DbTable::name).toList();
            assertThat(tableNames).contains("users", "orders", "products");
        }
    }

    @Test
    void getTables_shouldExcludeSystemSchemas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, true, null);

            // Should not contain tables from pg_catalog or information_schema
            List<String> schemas = tables.stream().map(DbTable::schema).distinct().toList();
            assertThat(schemas).doesNotContain("pg_catalog", "information_schema");
        }
    }

    @Test
    void getTables_shouldPopulateColumnsCorrectly() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

            DbTable usersTable = tables.stream()
                    .filter(t -> t.name().equals("users"))
                    .findFirst()
                    .orElseThrow();

            assertThat(usersTable.schema()).isEqualTo("public");
            assertThat(usersTable.dbColumns()).isNotEmpty();
            assertThat(usersTable.dbColumns().stream().map(c -> c.name()).toList())
                    .contains("id", "name", "email", "created_at");

            // Check primary key detection
            assertThat(usersTable.dbColumns().stream()
                    .filter(c -> c.name().equals("id"))
                    .findFirst()
                    .orElseThrow()
                    .pk()).isTrue();

            // Check auto-increment detection
            assertThat(usersTable.dbColumns().stream()
                    .filter(c -> c.name().equals("id"))
                    .findFirst()
                    .orElseThrow()
                    .autoIncremented()).isTrue();
        }
    }

    @Test
    void getTables_shouldSetCoverCharCorrectly() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

            DbTable table = tables.get(0);
            assertThat(table.coverChar()).isEqualTo("\"");
        }
    }

    @Test
    void getTables_shouldSetFullNameCorrectly() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("public"));

            DbTable usersTable = tables.stream()
                    .filter(t -> t.name().equals("users"))
                    .findFirst()
                    .orElseThrow();

            assertThat(usersTable.fullName()).isEqualTo("public.users");
        }
    }

    @Test
    void getTables_emptySchema_shouldReturnEmptyList() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement stmt = conn.createStatement()) {

            stmt.execute("CREATE SCHEMA IF NOT EXISTS empty_schema");
        }

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            List<DbTable> tables = dataExclusion.getTables(metaData, false, List.of("empty_schema"));

            assertThat(tables).isEmpty();
        }
    }

    @Test
    void getTables_withDatabaseMetaDataException_shouldThrowRuntimeException() throws SQLException {
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        when(mockMetaData.getSchemas()).thenThrow(new SQLException("Connection error"));

        assertThatThrownBy(() -> dataExclusion.getTables(mockMetaData, true, null))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(SQLException.class);
    }

    @Test
    void getDbTable_withNullSchema_shouldUseCatalogAsSchemaName() throws Exception {
        // Use reflection to test private getDbTable method with null schema
        Method getDbTableMethod = PostgreSQLDataExclusion.class.getDeclaredMethod(
                "getDbTable", DatabaseMetaData.class, MetaDataTable.class);
        getDbTableMethod.setAccessible(true);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            // Create MetaDataTable with null schema
            MetaDataTable metaDataTableNullSchema = new MetaDataTable(
                    "users", "testdb", null, "TABLE", "u"
            );

            DbTable result = (DbTable) getDbTableMethod.invoke(dataExclusion, metaData, metaDataTableNullSchema);

            // When schema is null, catalog should be used as schemaName
            assertThat(result.schema()).isEqualTo("testdb");
        }
    }

    @Test
    void getDbTable_withBlankSchema_shouldUseCatalogAsSchemaName() throws Exception {
        // Use reflection to test private getDbTable method with blank schema
        Method getDbTableMethod = PostgreSQLDataExclusion.class.getDeclaredMethod(
                "getDbTable", DatabaseMetaData.class, MetaDataTable.class);
        getDbTableMethod.setAccessible(true);

        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            DatabaseMetaData metaData = conn.getMetaData();

            // Create MetaDataTable with blank schema
            MetaDataTable metaDataTableBlankSchema = new MetaDataTable(
                    "users", "testdb", "   ", "TABLE", "u"
            );

            DbTable result = (DbTable) getDbTableMethod.invoke(dataExclusion, metaData, metaDataTableBlankSchema);

            // When schema is blank, catalog should be used as schemaName
            assertThat(result.schema()).isEqualTo("testdb");
        }
    }

    @Test
    void getDbTable_withSqlException_shouldThrowRuntimeException() throws Exception {
        // Use reflection to test private getDbTable method
        Method getDbTableMethod = PostgreSQLDataExclusion.class.getDeclaredMethod(
                "getDbTable", DatabaseMetaData.class, MetaDataTable.class);
        getDbTableMethod.setAccessible(true);

        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        when(mockMetaData.getPrimaryKeys(any(), any(), any())).thenThrow(new SQLException("Primary key error"));

        MetaDataTable metaDataTable = new MetaDataTable(
                "test_table", "testdb", "public", "TABLE", "t"
        );

        assertThatThrownBy(() -> getDbTableMethod.invoke(dataExclusion, mockMetaData, metaDataTable))
                .hasCauseInstanceOf(RuntimeException.class);
    }
}
