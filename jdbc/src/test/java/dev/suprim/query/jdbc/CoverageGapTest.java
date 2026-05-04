package dev.suprim.query.jdbc;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.config.*;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.JdbcOperationService;
import dev.suprim.query.jdbc.operation.support.JdbcMetaDataProvider;
import dev.suprim.query.model.ArrayTypeValueHolder;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.dto.CreateBulkResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.support.MetaDataExtraction;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoverageGapTest {

    // ==================== JdbcMetaDataProvider Tests ====================

    @Nested
    @DisplayName("JdbcMetaDataProvider")
    class JdbcMetaDataProviderTests {

        @Mock
        private DatabaseMetaData databaseMetaData;

        @Mock
        private MetaDataExtraction extraction;

        @Test
        void processMetaData_withMatchingExtraction_returnsDbMeta() throws Exception {
            when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(16);
            when(databaseMetaData.getDatabaseProductVersion()).thenReturn("16.2");
            when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(databaseMetaData.getDriverVersion()).thenReturn("42.7.1");

            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(databaseMetaData), eq(false), eq(List.of("public"))))
                    .thenReturn(List.of());

            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));
            provider.addExtraction(extraction);

            DbMeta result = provider.processMetaData(databaseMetaData);

            assertThat(result.productName()).isEqualTo("PostgreSQL");
            assertThat(result.majorVersion()).isEqualTo(16);
            assertThat(result.driverName()).isEqualTo("PostgreSQL JDBC Driver");
            assertThat(result.driverVersion()).isEqualTo("42.7.1");
        }

        @Test
        void processMetaData_noMatchingExtraction_throwsRuntimeException() throws Exception {
            when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
            when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(8);
            when(databaseMetaData.getDatabaseProductVersion()).thenReturn("8.0");
            when(databaseMetaData.getDriverName()).thenReturn("MySQL Driver");
            when(databaseMetaData.getDriverVersion()).thenReturn("8.0.33");

            when(extraction.canHandle("MySQL")).thenReturn(false);

            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(true, List.of());
            provider.addExtraction(extraction);

            assertThatThrownBy(() -> provider.processMetaData(databaseMetaData))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No extractor");
        }

        @Test
        void processMetaData_noExtractions_throwsRuntimeException() throws Exception {
            when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(16);
            when(databaseMetaData.getDatabaseProductVersion()).thenReturn("16.2");
            when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(databaseMetaData.getDriverVersion()).thenReturn("42.7.1");

            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));

            assertThatThrownBy(() -> provider.processMetaData(databaseMetaData))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void processMetaData_withAllSchemas_passesCorrectFlag() throws Exception {
            when(databaseMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(databaseMetaData.getDatabaseMajorVersion()).thenReturn(16);
            when(databaseMetaData.getDatabaseProductVersion()).thenReturn("16.2");
            when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(databaseMetaData.getDriverVersion()).thenReturn("42.7.1");

            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(databaseMetaData), eq(true), eq(List.of())))
                    .thenReturn(List.of());

            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(true, List.of());
            provider.addExtraction(extraction);

            DbMeta result = provider.processMetaData(databaseMetaData);

            assertThat(result.dbTables()).isEmpty();
            verify(extraction).getTables(databaseMetaData, true, List.of());
        }

        @Test
        void processMetaData_nullDatabaseMetaData_throwsNullPointerException() {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(true, List.of());
            provider.addExtraction(extraction);

            assertThatThrownBy(() -> provider.processMetaData(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================== JdbcManager additional paths ====================

    @Nested
    @DisplayName("JdbcManager additional coverage")
    class JdbcManagerAdditionalTests {

        @Test
        void getDbMetaByDbId_returnsDbMeta() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of());

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            DbMeta meta = mgr.getDbMetaByDbId("db1");
            assertThat(meta).isNotNull();
            assertThat(meta.productName()).isEqualTo("PostgreSQL");
        }

        @Test
        void getDbMetaMap_returnsAllMetas() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of());

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            Map<String, DbMeta> metaMap = mgr.getDbMetaMap();
            assertThat(metaMap).containsKey("db1");
        }

        @Test
        void getTables_validDbId_returnsTables() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of(table));

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            List<DbTable> tables = mgr.getTables("db1");
            assertThat(tables).hasSize(1);
            assertThat(tables.get(0).name()).isEqualTo("users");
        }

        @Test
        void getTables_invalidDbId_throwsDbException() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");

            JdbcManager mgr = new JdbcManager(mockDs, List.of(), props, List.of());

            assertThatThrownBy(() -> mgr.getTables("nonexistent"))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void getTable_withSchemaAndInvalidDbId_throwsDbException() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");

            JdbcManager mgr = new JdbcManager(mockDs, List.of(), props, List.of());

            assertThatThrownBy(() -> mgr.getTable("nonexistent", "public", "users"))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void getTable_blankSchemaName_searchesByTableNameOnly() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of(table));

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            // Blank schema should fall through to search by table name only
            DbTable result = mgr.getTable("db1", "   ", "users");
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("users");
        }

        @Test
        void reload_successfulLoad_completesWithoutException() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of());

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));

            // reload() calls loadAllMetaData() — success path (line 46)
            assertThatCode(mgr::reload).doesNotThrowAnyException();
        }

        @Test
        void loadAllMetaData_noDialectFound_throwsDbRuntimeException() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("UnknownDB");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(1);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("1.0");
            when(mockMeta.getDriverName()).thenReturn("Unknown Driver");
            when(mockMeta.getDriverVersion()).thenReturn("1.0");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("UnknownDB", 1)).thenReturn(false);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("UnknownDB")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of());

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "unknowndb", "jdbc:test", "u", "p", "testdb",
                    null, null, null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));

            assertThatThrownBy(mgr::loadAllMetaData)
                    .isInstanceOf(dev.suprim.query.exception.DbRuntimeException.class);
        }

        @Test
        void loadAllMetaData_nullConnectionDetail_usesDefaults() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), eq(true), isNull()))
                    .thenReturn(List.of());

            // Empty databases list so getDatabase returns empty
            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of());

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            // Should succeed with includeAllSchemas=true and schemas=null
            assertThat(mgr.getDbMetaByDbId("db1")).isNotNull();
        }

        @Test
        void getTable_withSchema_tableNotFound_throwsDbException() throws Exception {
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            DatabaseMetaData mockMeta = mock(DatabaseMetaData.class);

            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.getMetaData()).thenReturn(mockMeta);
            when(mockMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(mockMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(mockMeta.getDatabaseProductVersion()).thenReturn("16.2");
            when(mockMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(mockMeta.getDriverVersion()).thenReturn("42.7.1");

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            Dialect dialect = mock(Dialect.class);
            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);

            MetaDataExtraction extraction = mock(MetaDataExtraction.class);
            when(extraction.canHandle("PostgreSQL")).thenReturn(true);
            when(extraction.getTables(eq(mockMeta), anyBoolean(), any()))
                    .thenReturn(List.of(table));

            DatabaseProperties props = new DatabaseProperties();
            props.setDefaultDatabaseId("db1");
            props.setDatabases(List.of(new DatabaseConnectionDetail(
                    "db1", "postgresql", "jdbc:test", "u", "p", "testdb",
                    null, List.of("public"), null, null, null, 5
            )));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mockDs);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            JdbcManager mgr = new JdbcManager(routingDs, List.of(dialect), props, List.of(extraction));
            mgr.loadAllMetaData();

            // Schema matches but table name doesn't — covers L219 false branch
            assertThatThrownBy(() -> mgr.getTable("db1", "public", "nonexistent"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Missing table");
        }
    }

    // ==================== JdbcOperationService - processArrayValue ====================

    @Nested
    @DisplayName("JdbcOperationService processArrayValue")
    class JdbcOperationServiceArrayTests {

        @Test
        void create_withArrayTypeValueHolder_processesArray() throws Exception {
            JdbcOperationService service = new JdbcOperationService();

            NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
            JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);
            Array mockArray = mock(Array.class);

            when(mockTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
            when(mockJdbcTemplate.getDataSource()).thenReturn(mockDs);
            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.createArrayOf(eq("varchar"), any(Object[].class))).thenReturn(mockArray);

            // Mock the update call
            when(mockTemplate.update(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class),
                    any(KeyHolder.class), any(String[].class))).thenReturn(1);

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            Map<String, Object> data = new HashMap<>();
            data.put("tags", new ArrayTypeValueHolder("varchar", "varchar", new Object[]{"a", "b"}));

            CreationResponse response = service.create(mockTemplate, data, "INSERT INTO users (tags) VALUES (:tags)", table);

            assertThat(response).isNotNull();
            assertThat(response.row()).isEqualTo(1);
        }

        @Test
        void create_withArrayTypeValueHolder_sqlException_throwsDbException() throws Exception {
            JdbcOperationService service = new JdbcOperationService();

            NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
            JdbcTemplate mockJdbcTemplate = mock(JdbcTemplate.class);
            DataSource mockDs = mock(DataSource.class);
            Connection mockConn = mock(Connection.class);

            when(mockTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
            when(mockJdbcTemplate.getDataSource()).thenReturn(mockDs);
            when(mockDs.getConnection()).thenReturn(mockConn);
            when(mockConn.createArrayOf(anyString(), any(Object[].class)))
                    .thenThrow(new SQLException("Array creation failed"));

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            Map<String, Object> data = new HashMap<>();
            data.put("tags", new ArrayTypeValueHolder("varchar", "varchar", new Object[]{"a"}));

            assertThatThrownBy(() -> service.create(mockTemplate, data,
                    "INSERT INTO users (tags) VALUES (:tags)", table))
                    .isInstanceOf(DbException.class);
        }

        @Test
        void batchUpdate_withKeyHolder_returnsBulkResponse() {
            JdbcOperationService service = new JdbcOperationService();

            NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
            when(mockTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class),
                    any(KeyHolder.class), any(String[].class)))
                    .thenReturn(new int[]{1, 1});

            DbTable table = new DbTable("public", "users", "public.users", "u",
                    List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            List<Map<String, Object>> dataList = List.of(
                    Map.of("name", "A"),
                    Map.of("name", "B")
            );

            CreateBulkResponse response = service.batchUpdate(mockTemplate, dataList,
                    "INSERT INTO users (name) VALUES (:name)", table);

            assertThat(response).isNotNull();
            assertThat(response.rows()).hasSize(2);
        }

        @Test
        void batchUpdate_withoutKeyHolder_returnsBulkResponse() {
            JdbcOperationService service = new JdbcOperationService();

            NamedParameterJdbcTemplate mockTemplate = mock(NamedParameterJdbcTemplate.class);
            when(mockTemplate.batchUpdate(anyString(), any(SqlParameterSource[].class)))
                    .thenReturn(new int[]{1, 1, 1});

            List<Map<String, Object>> dataList = List.of(
                    Map.of("name", "A"),
                    Map.of("name", "B"),
                    Map.of("name", "C")
            );

            CreateBulkResponse response = service.batchUpdate(mockTemplate, dataList,
                    "INSERT INTO users (name) VALUES (:name)");

            assertThat(response).isNotNull();
            assertThat(response.rows()).hasSize(3);
            assertThat(response.keys()).isNull();
        }
    }

    // ==================== RoutingDataSource - additional paths ====================

    @Nested
    @DisplayName("RoutingDataSource additional paths")
    class RoutingDataSourceAdditionalTests {

        @Test
        void knownIds_containsAllConfiguredIds() {
            DataSource ds1 = mock(DataSource.class);
            DataSource ds2 = mock(DataSource.class);
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", ds1);
            targets.put("db2", ds2);

            RoutingDataSource routingDs = new RoutingDataSource(targets, "db1");

            assertThat(routingDs.getKnownIds()).containsExactlyInAnyOrder("db1", "db2");
            assertThat(routingDs.getDefaultId()).isEqualTo("db1");
        }

        @Test
        void constructor_nullTargets_throwsNullPointerException() {
            assertThatThrownBy(() -> new RoutingDataSource(null, "db1"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void constructor_nullDefaultId_throwsNullPointerException() {
            Map<String, DataSource> targets = new HashMap<>();
            targets.put("db1", mock(DataSource.class));

            assertThatThrownBy(() -> new RoutingDataSource(targets, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // ==================== DatabaseContextHolder - static method ====================

    @Nested
    @DisplayName("DatabaseContextHolder static coverage")
    class DatabaseContextHolderStaticTests {

        @Test
        void getCurrentDbId_whenNeverSet_returnsNull() {
            DatabaseContextHolder.clear();
            assertThat(DatabaseContextHolder.getCurrentDbId()).isNull();
        }
    }
}
