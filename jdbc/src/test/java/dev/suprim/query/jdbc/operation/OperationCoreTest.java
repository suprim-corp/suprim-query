package dev.suprim.query.jdbc.operation;

import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbErrorCode;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.exception.DbRuntimeException;
import dev.suprim.query.jdbc.config.*;
import dev.suprim.query.model.*;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreateBulkResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.model.dto.ExistsResponse;
import dev.suprim.query.support.MetaDataExtraction;
import gg.jte.TemplateEngine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OperationCoreTest {

    @Nested
    @DisplayName("JdbcManager Tests")
    class JdbcManagerTests {

        @Mock
        private DataSource simpleDataSource;
        @Mock
        private Dialect dialect;
        @Mock
        private MetaDataExtraction metaDataExtraction;

        private DatabaseProperties databaseProperties;

        @BeforeEach
        void setup() {
            databaseProperties = new DatabaseProperties();
            databaseProperties.setDefaultDatabaseId("test");
            databaseProperties.setDatabases(List.of(
                    new DatabaseConnectionDetail(
                            "test", "postgresql", "jdbc:postgresql://localhost/test",
                            "user", "pass", "testdb", null, List.of("public"), null, null,
                            new EnvironmentProperties(false, "HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", 100),
                            5
                    )
            ));
        }

        @Test
        void loadAllMetaData_nonRoutingDataSource_doesNothing() {
            JdbcManager manager = new JdbcManager(simpleDataSource, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            // Should not throw — just logs and returns
            assertThatCode(manager::loadAllMetaData).doesNotThrowAnyException();
        }

        @Test
        void loadAllMetaData_routingDataSource_loadsMetaData() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);

            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of(table));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            assertThat(manager.getNamedParameterJdbcTemplate("test")).isNotNull();
            assertThat(manager.getTxnTemplate("test")).isNotNull();
        }

        @Test
        void getTable_validTable_returnsTable() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);

            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of(table));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            DbTable result = manager.getTable("test", null, "users");
            assertThat(result.name()).isEqualTo("users");
        }

        @Test
        void getTable_withSchema_returnsTable() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);

            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of(table));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            DbTable result = manager.getTable("test", "public", "users");
            assertThat(result.name()).isEqualTo("users");
        }

        @Test
        void getTable_invalidTable_throwsDbException() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);

            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of(table));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            assertThatThrownBy(() -> manager.getTable("test", null, "nonexistent"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Invalid table name");
        }

        @Test
        void getTable_invalidDbId_throwsDbException() {
            JdbcManager manager = new JdbcManager(simpleDataSource, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            assertThatThrownBy(() -> manager.getTable("unknown", null, "users"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("DB not found");
        }

        @Test
        void getDialect_validDbId_returnsDialect() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of());

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            Dialect result = manager.getDialect("test");
            assertThat(result).isEqualTo(dialect);
        }

        @Test
        void getDialect_invalidDbId_throwsDbException() {
            JdbcManager manager = new JdbcManager(simpleDataSource, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            assertThatThrownBy(() -> manager.getDialect("unknown"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("DB not found");
        }

        @Test
        void getTables_empty_returnsEmptyList() {
            JdbcManager manager = new JdbcManager(simpleDataSource, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            assertThat(manager.getTables()).isEmpty();
        }

        @Test
        void getTables_byDbId_invalidDbId_throwsDbException() {
            JdbcManager manager = new JdbcManager(simpleDataSource, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            assertThatThrownBy(() -> manager.getTables("unknown"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("DB not found");
        }

        @Test
        void reload_dbUnavailable_doesNotThrow() throws Exception {
            DataSource ds = mock(DataSource.class);
            when(ds.getConnection()).thenThrow(new SQLException("Connection refused"));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));

            // reload() catches DbRuntimeException and logs it
            assertThatCode(manager::reload).doesNotThrowAnyException();
        }

        @Test
        void getTable_withSchemaInvalid_throwsDbException() throws Exception {
            DataSource ds = mock(DataSource.class);
            Connection conn = mock(Connection.class);
            DatabaseMetaData dbMeta = mock(DatabaseMetaData.class);

            when(ds.getConnection()).thenReturn(conn);
            when(conn.getMetaData()).thenReturn(dbMeta);
            when(dbMeta.getDatabaseProductName()).thenReturn("PostgreSQL");
            when(dbMeta.getDatabaseMajorVersion()).thenReturn(16);
            when(dbMeta.getDatabaseProductVersion()).thenReturn("16.0");
            when(dbMeta.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
            when(dbMeta.getDriverVersion()).thenReturn("42.7.8");

            when(dialect.isSupportedDb("PostgreSQL", 16)).thenReturn(true);
            when(metaDataExtraction.canHandle("PostgreSQL")).thenReturn(true);

            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");
            when(metaDataExtraction.getTables(eq(dbMeta), anyBoolean(), anyList()))
                    .thenReturn(List.of(table));

            Map<String, DataSource> targets = new HashMap<>();
            targets.put("test", ds);
            RoutingDataSource routingDs = new RoutingDataSource(targets, "test");

            JdbcManager manager = new JdbcManager(routingDs, List.of(dialect), databaseProperties, List.of(metaDataExtraction));
            manager.loadAllMetaData();

            assertThatThrownBy(() -> manager.getTable("test", "other_schema", "users"))
                    .isInstanceOf(DbException.class)
                    .hasMessageContaining("Missing table");
        }
    }

    @Nested
    @DisplayName("JdbcOperationService Tests")
    class JdbcOperationServiceTests {

        private JdbcOperationService operationService;
        @Mock
        private NamedParameterJdbcTemplate jdbcTemplate;
        @Mock
        private Dialect dialect;

        @BeforeEach
        void setup() {
            operationService = new JdbcOperationService();
        }

        @Test
        void update_returnsRowCount() {
            when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(3);

            int result = operationService.update(jdbcTemplate, Map.of("name", "test"), "UPDATE users SET name = :name");

            assertThat(result).isEqualTo(3);
        }

        @Test
        void read_returnsList() {
            when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(SimpleRowMapper.class)))
                    .thenReturn(List.of(Map.of("id", 1L)));

            List<Map<String, Object>> result = operationService.read(
                    jdbcTemplate, Map.of(), "SELECT * FROM users", dialect
            );

            assertThat(result).hasSize(1);
        }

        @Test
        void findOne_returnsMap() {
            when(jdbcTemplate.queryForMap(anyString(), anyMap()))
                    .thenReturn(Map.of("id", 1L, "name", "John"));

            Map<String, Object> result = operationService.findOne(jdbcTemplate, "SELECT * FROM users WHERE id = :id", Map.of("id", 1L));

            assertThat(result).containsEntry("name", "John");
        }

        @Test
        void exists_emptyResult_returnsFalse() {
            when(jdbcTemplate.query(anyString(), anyMap(), any(org.springframework.jdbc.core.RowMapper.class)))
                    .thenReturn(List.of());

            ExistsResponse result = operationService.exists(jdbcTemplate, Map.of(), "SELECT 1 FROM users WHERE id = :id");

            assertThat(result.exists()).isFalse();
        }

        @Test
        void exists_withResult_returnsTrue() {
            when(jdbcTemplate.query(anyString(), anyMap(), any(org.springframework.jdbc.core.RowMapper.class)))
                    .thenReturn(List.of("1"));

            ExistsResponse result = operationService.exists(jdbcTemplate, Map.of(), "SELECT 1 FROM users WHERE id = :id");

            assertThat(result.exists()).isTrue();
        }

        @Test
        @SuppressWarnings("unchecked")
        void exists_rowMapperLambda_extractsString() throws Exception {
            // Use Answer to invoke the RowMapper lambda to cover L77
            ResultSet mockRs = mock(ResultSet.class);
            when(mockRs.getString(1)).thenReturn("1");

            when(jdbcTemplate.query(anyString(), anyMap(), any(org.springframework.jdbc.core.RowMapper.class)))
                    .thenAnswer(invocation -> {
                        org.springframework.jdbc.core.RowMapper<String> mapper = invocation.getArgument(2);
                        String value = mapper.mapRow(mockRs, 0);
                        return List.of(value);
                    });

            ExistsResponse result = operationService.exists(jdbcTemplate, Map.of(), "SELECT 1 FROM users WHERE id = :id");

            assertThat(result.exists()).isTrue();
            verify(mockRs).getString(1);
        }

        @Test
        void count_returnsCount() {
            when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(42L);

            CountResponse result = operationService.count(jdbcTemplate, Map.of(), "SELECT COUNT(*) FROM users");

            assertThat(result.count()).isEqualTo(42);
        }

        @Test
        void count_nullResult_returnsZero() {
            when(jdbcTemplate.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(null);

            CountResponse result = operationService.count(jdbcTemplate, Map.of(), "SELECT COUNT(*) FROM users");

            assertThat(result.count()).isEqualTo(0);
        }

        @Test
        void queryCustom_single_returnsMap() {
            when(jdbcTemplate.queryForMap(anyString(), anyMap())).thenReturn(Map.of("id", 1L));

            Object result = operationService.queryCustom(jdbcTemplate, true, "SELECT * FROM users LIMIT 1", Map.of());

            assertThat(result).isInstanceOf(Map.class);
        }

        @Test
        void queryCustom_list_returnsList() {
            when(jdbcTemplate.queryForList(anyString(), anyMap())).thenReturn(List.of(Map.of("id", 1L)));

            Object result = operationService.queryCustom(jdbcTemplate, false, "SELECT * FROM users", Map.of());

            assertThat(result).isInstanceOf(List.class);
        }

        @Test
        void delete_returnsRowCount() {
            when(jdbcTemplate.update(anyString(), anyMap())).thenReturn(2);

            int result = operationService.delete(jdbcTemplate, Map.of("id", 1), "DELETE FROM users WHERE id = :id");

            assertThat(result).isEqualTo(2);
        }

        @Test
        void create_simple_returnsCreationResponse() throws DbException {
            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            when(jdbcTemplate.update(anyString(), any(MapSqlParameterSource.class), any(KeyHolder.class), any(String[].class)))
                    .thenReturn(1);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", "John");

            CreationResponse result = operationService.create(jdbcTemplate, data, "INSERT INTO users (name) VALUES (:name)", table);

            assertThat(result.row()).isEqualTo(1);
        }

        @Test
        void batchUpdate_withKeyHolder_returnsResponse() {
            DbTable table = new DbTable("public", "users", "\"public\".\"users\"", "t0",
                    List.of(new DbColumn("users", "id", "", "t0", true, "int8", false, false, Long.class, "\"", "")),
                    "TABLE", "\"");

            when(jdbcTemplate.batchUpdate(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource[].class), any(KeyHolder.class), any(String[].class)))
                    .thenReturn(new int[]{1, 1});

            List<Map<String, Object>> dataList = List.of(
                    Map.of("name", "John"),
                    Map.of("name", "Jane")
            );

            CreateBulkResponse result = operationService.batchUpdate(jdbcTemplate, dataList, "INSERT INTO users (name) VALUES (:name)", table);

            assertThat(result.rows()).hasSize(2);
        }

        @Test
        void batchUpdate_withoutKeyHolder_returnsResponse() {
            when(jdbcTemplate.batchUpdate(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource[].class)))
                    .thenReturn(new int[]{1, 1});

            List<Map<String, Object>> dataList = List.of(
                    Map.of("name", "John"),
                    Map.of("name", "Jane")
            );

            CreateBulkResponse result = operationService.batchUpdate(jdbcTemplate, dataList, "INSERT INTO users (name) VALUES (:name)");

            assertThat(result.rows()).hasSize(2);
            assertThat(result.keys()).isNull();
        }
    }

    @Nested
    @DisplayName("SimpleRowMapper Tests")
    class SimpleRowMapperTests {

        @Mock
        private Dialect dialect;

        @Test
        void getColumnValue_standardType_callsSuper() throws Exception {
            SimpleRowMapper mapper = new SimpleRowMapper(dialect);

            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("varchar");
            when(rs.getObject(1)).thenReturn("hello");

            Object result = mapper.getColumnValue(rs, 1);

            assertThat(result).isEqualTo("hello");
        }

        @Test
        void getColumnValue_jsonType_callsDialectConvertJson() throws Exception {
            SimpleRowMapper mapper = new SimpleRowMapper(dialect);

            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("jsonb");
            when(rs.getObject(1)).thenReturn("{}");
            when(dialect.convertJsonToVO("{}")).thenReturn(Map.of());

            Object result = mapper.getColumnValue(rs, 1);

            assertThat(result).isEqualTo(Map.of());
            verify(dialect).convertJsonToVO("{}");
        }

        @Test
        void getColumnValue_jsonType_dbException_throwsRuntimeException() throws Exception {
            SimpleRowMapper mapper = new SimpleRowMapper(dialect);

            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("json");
            when(rs.getObject(1)).thenReturn("invalid");
            when(dialect.convertJsonToVO("invalid")).thenThrow(new DbException(DbErrorCode.SERVER_ERROR));

            assertThatThrownBy(() -> mapper.getColumnValue(rs, 1))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        void getColumnValue_varcharArrayType_callsDialectConvertArray() throws Exception {
            SimpleRowMapper mapper = new SimpleRowMapper(dialect);

            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            Array sqlArray = mock(Array.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("_varchar");
            when(rs.getArray(1)).thenReturn(sqlArray);
            when(dialect.convertToStringArray(sqlArray)).thenReturn(List.of("a", "b"));

            Object result = mapper.getColumnValue(rs, 1);

            assertThat(result).isEqualTo(List.of("a", "b"));
            verify(dialect).convertToStringArray(sqlArray);
        }

        @Test
        void getColumnValue_varcharArrayType_dbException_throwsRuntimeException() throws Exception {
            SimpleRowMapper mapper = new SimpleRowMapper(dialect);

            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);
            Array sqlArray = mock(Array.class);
            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("_varchar");
            when(rs.getArray(1)).thenReturn(sqlArray);
            when(dialect.convertToStringArray(sqlArray)).thenThrow(new DbException(DbErrorCode.SERVER_ERROR));

            assertThatThrownBy(() -> mapper.getColumnValue(rs, 1))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("SqlCreatorTemplate Tests")
    class SqlCreatorTemplateTests {

        @Mock
        private TemplateEngine templateEngine;
        @Mock
        private JdbcManager jdbcManager;

        @Test
        void accessors_returnCorrectValues() {
            SqlCreatorTemplate template = new SqlCreatorTemplate(templateEngine, jdbcManager);

            assertThat(template.templateEngine()).isEqualTo(templateEngine);
            assertThat(template.jdbcManager()).isEqualTo(jdbcManager);
        }
    }
}
