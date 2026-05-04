package dev.suprim.query.jdbc.operation;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.EnvironmentProperties;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.model.ArrayTypeValueHolder;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.dto.CountResponse;
import dev.suprim.query.model.dto.CreateBulkResponse;
import dev.suprim.query.model.dto.CreationResponse;
import dev.suprim.query.model.dto.ExistsResponse;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.DirectoryCodeResolver;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OperationCoreTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static HikariDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static Dialect dialect;
    private static JdbcManager jdbcManager;
    private static JdbcOperationService operationService;

    @BeforeAll
    static void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setAutoCommit(true);
        config.setPoolName("testPool");

        dataSource = new HikariDataSource(config);
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        ObjectMapper objectMapper = new ObjectMapper();
        dialect = new PostGreSQLDialect(objectMapper);

        DatabaseProperties dbProperties = new DatabaseProperties();
        dbProperties.setDefaultDatabaseId("test");
        dbProperties.setDatabases(List.of(
                new DatabaseConnectionDetail(
                        "test", "postgresql",
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword(),
                        "testdb",
                        null, List.of("public"), null, null,
                        new EnvironmentProperties(false, "HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", 100),
                        5
                )
        ));

        Map<String, DataSource> targets = new HashMap<>();
        targets.put("test", dataSource);
        RoutingDataSource routingDataSource = new RoutingDataSource(targets, "test");

        // Create tables FIRST before loading metadata
        createTestTables();

        List<MetaDataExtraction> metaDataExtractions = List.of(new PostgreSQLDataExclusion());
        jdbcManager = new JdbcManager(routingDataSource, List.of(dialect), dbProperties, metaDataExtractions);
        jdbcManager.reload();

        operationService = new JdbcOperationService();

        // Insert base test data
        insertBaseTestData();
    }

    private static void createTestTables() {
        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.test_users (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE,
                age INT,
                metadata JSONB,
                tags VARCHAR[] DEFAULT '{}',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """);

        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.departments (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.empty_table (
                id INT
            )
            """);
    }

    private static void insertBaseTestData() {
        jdbcTemplate.getJdbcOperations().execute("DELETE FROM public.test_users");
        jdbcTemplate.getJdbcOperations().execute("DELETE FROM public.departments");

        // Insert base user for SimpleRowMapper and other tests
        jdbcTemplate.getJdbcOperations().execute("""
            INSERT INTO public.test_users (id, name, email, age, metadata, tags)
            VALUES (1, 'Base User', 'base@test.com', 25, '{"role": "user"}'::jsonb, ARRAY['tag1', 'tag2'])
            """);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Nested
    @DisplayName("JdbcManager Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class JdbcManagerTests {

        @Test
        @Order(1)
        void loadAllMetaData_withRoutingDataSource_loadsMetadata() {
            assertNotNull(jdbcManager.getDbMetaMap());
            assertFalse(jdbcManager.getDbMetaMap().isEmpty());
            assertNotNull(jdbcManager.getDbMetaByDbId("test"));
        }

        @Test
        @Order(2)
        void loadAllMetaData_withNonRoutingDataSource_skips() {
            DatabaseProperties props = new DatabaseProperties();
            props.setDatabases(List.of());

            JdbcManager nonRoutingManager = new JdbcManager(
                    dataSource,
                    List.of(dialect),
                    props,
                    List.of()
            );
            nonRoutingManager.reload();

            assertTrue(nonRoutingManager.getDbMetaMap().isEmpty());
        }

        @Test
        @Order(3)
        void getTable_validTable_returnsTable() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "test_users");

            assertNotNull(table);
            assertEquals("test_users", table.name());
        }

        @Test
        @Order(4)
        void getTable_withSchema_returnsTable() throws DbException {
            DbTable table = jdbcManager.getTable("test", "public", "test_users");

            assertNotNull(table);
            assertEquals("test_users", table.name());
            assertEquals("public", table.schema());
        }

        @Test
        @Order(5)
        void getTable_invalidTable_throwsDbException() {
            DbException ex = assertThrows(DbException.class,
                    () -> jdbcManager.getTable("test", null, "nonexistent_table"));

            assertTrue(ex.getMessage().contains("Invalid table name"));
        }

        @Test
        @Order(6)
        void getTable_invalidDbId_throwsDbException() {
            DbException ex = assertThrows(DbException.class,
                    () -> jdbcManager.getTable("nonexistent_db", null, "test_users"));

            assertTrue(ex.getMessage().contains("DB not found"));
        }

        @Test
        @Order(7)
        void getTable_withSchemaAndInvalidTable_throwsDbException() {
            DbException ex = assertThrows(DbException.class,
                    () -> jdbcManager.getTable("test", "public", "nonexistent_table"));

            assertTrue(ex.getMessage().contains("Missing table"));
        }

        @Test
        @Order(8)
        void getDialect_validDbId_returnsDialect() throws DbException {
            Dialect result = jdbcManager.getDialect("test");

            assertNotNull(result);
            assertTrue(result instanceof PostGreSQLDialect);
        }

        @Test
        @Order(9)
        void getDialect_invalidDbId_throwsDbException() {
            DbException ex = assertThrows(DbException.class,
                    () -> jdbcManager.getDialect("nonexistent_db"));

            assertTrue(ex.getMessage().contains("DB not found"));
        }

        @Test
        @Order(10)
        void getNamedParameterJdbcTemplate_returnsTemplate() {
            NamedParameterJdbcTemplate template = jdbcManager.getNamedParameterJdbcTemplate("test");
            assertNotNull(template);
        }

        @Test
        @Order(11)
        void getTxnTemplate_returnsTemplate() {
            assertNotNull(jdbcManager.getTxnTemplate("test"));
        }

        @Test
        @Order(12)
        void getTables_returnsEmptyList() {
            List<DbTable> tables = jdbcManager.getTables();
            assertTrue(tables.isEmpty());
        }
    }

    @Nested
    @DisplayName("JdbcOperationService Tests")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class JdbcOperationServiceTests {

        @Test
        @Order(1)
        void update_executesAndReturnsRowCount() {
            jdbcTemplate.update(
                    "INSERT INTO public.test_users (id, name, email, age) VALUES (:id, :name, :email, :age)",
                    Map.of("id", 100L, "name", "Test User", "email", "test@test.com", "age", 25)
            );

            int rows = operationService.update(
                    jdbcTemplate,
                    Map.of("id", 100L, "age", 30),
                    "UPDATE public.test_users SET age = :age WHERE id = :id"
            );

            assertEquals(1, rows);
        }

        @Test
        @Order(2)
        void read_returnsListOfMaps() {
            List<Map<String, Object>> results = operationService.read(
                    jdbcTemplate,
                    Map.of("id", 100L),
                    "SELECT id, name, email, age FROM public.test_users WHERE id = :id",
                    dialect
            );

            assertFalse(results.isEmpty());
            assertEquals("Test User", results.get(0).get("name"));
            assertEquals(30, results.get(0).get("age"));
        }

        @Test
        @Order(3)
        void findOne_returnsMap() {
            Map<String, Object> result = operationService.findOne(
                    jdbcTemplate,
                    "SELECT id, name FROM public.test_users WHERE id = :id",
                    Map.of("id", 100L)
            );

            assertNotNull(result);
            assertEquals("Test User", result.get("name"));
        }

        @Test
        @Order(4)
        void exists_emptyResult_returnsFalse() {
            ExistsResponse response = operationService.exists(
                    jdbcTemplate,
                    Map.of("id", 99999L),
                    "SELECT 1 FROM public.test_users WHERE id = :id"
            );

            assertFalse(response.exists());
        }

        @Test
        @Order(5)
        void exists_withResult_returnsTrue() {
            ExistsResponse response = operationService.exists(
                    jdbcTemplate,
                    Map.of("id", 100L),
                    "SELECT 1 FROM public.test_users WHERE id = :id"
            );

            assertTrue(response.exists());
        }

        @Test
        @Order(6)
        void count_returnsCount() {
            CountResponse response = operationService.count(
                    jdbcTemplate,
                    Map.of(),
                    "SELECT COUNT(*) FROM public.test_users"
            );

            assertTrue(response.count() >= 1);
        }

        @Test
        @Order(7)
        void count_nullResult_returnsZero() {
            jdbcTemplate.getJdbcOperations().execute("CREATE TABLE IF NOT EXISTS public.empty_table (id INT)");

            CountResponse response = operationService.count(
                    jdbcTemplate,
                    Map.of(),
                    "SELECT COUNT(*) FROM public.empty_table WHERE 1=0"
            );

            assertEquals(0, response.count());
        }

        @Test
        @Order(8)
        void queryCustom_single_returnsMap() {
            Object result = operationService.queryCustom(
                    jdbcTemplate,
                    true,
                    "SELECT id, name FROM public.test_users WHERE id = :id",
                    Map.of("id", 100L)
            );

            assertInstanceOf(Map.class, result);
        }

        @Test
        @Order(9)
        void queryCustom_list_returnsList() {
            Object result = operationService.queryCustom(
                    jdbcTemplate,
                    false,
                    "SELECT id, name FROM public.test_users",
                    Map.of()
            );

            assertInstanceOf(List.class, result);
        }

        @Test
        @Order(10)
        void create_insertsAndReturnsKeys() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "test_users");

            CreationResponse response = operationService.create(
                    jdbcTemplate,
                    Map.of("id", 101L, "name", "Created User", "email", "created@test.com", "age", 35),
                    "INSERT INTO public.test_users (id, name, email, age) VALUES (:id, :name, :email, :age)",
                    table
            );

            assertEquals(1, response.row());
        }

        @Test
        @Order(11)
        void create_withArrayType_processesArray() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "test_users");

            String[] tags = {"tag1", "tag2", "tag3"};
            ArrayTypeValueHolder arrayHolder = new ArrayTypeValueHolder("varchar", "varchar", tags);

            Map<String, Object> data = new HashMap<>();
            data.put("id", 102L);
            data.put("name", "Array User");
            data.put("email", "array@test.com");
            data.put("age", 40);
            data.put("tags", arrayHolder);

            CreationResponse response = operationService.create(
                    jdbcTemplate,
                    data,
                    "INSERT INTO public.test_users (id, name, email, age, tags) VALUES (:id, :name, :email, :age, :tags)",
                    table
            );

            assertEquals(1, response.row());
        }

        @Test
        @Order(12)
        void batchUpdate_withKeyHolder_returnsKeys() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "test_users");

            List<Map<String, Object>> dataList = List.of(
                    Map.of("id", 201L, "name", "Batch1", "email", "batch1@test.com", "age", 20),
                    Map.of("id", 202L, "name", "Batch2", "email", "batch2@test.com", "age", 21)
            );

            CreateBulkResponse response = operationService.batchUpdate(
                    jdbcTemplate,
                    dataList,
                    "INSERT INTO public.test_users (id, name, email, age) VALUES (:id, :name, :email, :age)",
                    table
            );

            assertEquals(2, response.rows().length);
        }

        @Test
        @Order(13)
        void batchUpdate_withoutKeyHolder_returnsNull() {
            List<Map<String, Object>> dataList = List.of(
                    Map.of("id", 203L, "name", "Batch3", "email", "batch3@test.com", "age", 22),
                    Map.of("id", 204L, "name", "Batch4", "email", "batch4@test.com", "age", 23)
            );

            CreateBulkResponse response = operationService.batchUpdate(
                    jdbcTemplate,
                    dataList,
                    "INSERT INTO public.test_users (id, name, email, age) VALUES (:id, :name, :email, :age)"
            );

            assertEquals(2, response.rows().length);
            assertNull(response.keys());
        }

        @Test
        @Order(14)
        void delete_deletesRecord() {
            int rows = operationService.delete(
                    jdbcTemplate,
                    Map.of("id", 203L),
                    "DELETE FROM public.test_users WHERE id = :id"
            );

            assertEquals(1, rows);
        }
    }

    @Nested
    @DisplayName("SimpleRowMapper Tests")
    class SimpleRowMapperTests {

        @Test
        void mapRow_standardColumns_mapsCorrectly() {
            List<Map<String, Object>> results = jdbcTemplate.query(
                    "SELECT id, name, email, age FROM public.test_users WHERE id = :id",
                    Map.of("id", 1L),
                    new SimpleRowMapper(dialect)
            );

            assertFalse(results.isEmpty());
            assertEquals("Base User", results.get(0).get("name"));
        }

        @Test
        void mapRow_jsonColumn_convertsToObject() {
            List<Map<String, Object>> results = jdbcTemplate.query(
                    "SELECT metadata FROM public.test_users WHERE id = :id",
                    Map.of("id", 1L),
                    new SimpleRowMapper(dialect)
            );

            assertFalse(results.isEmpty());
            assertNotNull(results.get(0).get("metadata"));
        }

        @Test
        void mapRow_arrayColumn_convertsList() {
            List<Map<String, Object>> results = jdbcTemplate.query(
                    "SELECT tags FROM public.test_users WHERE id = :id",
                    Map.of("id", 1L),
                    new SimpleRowMapper(dialect)
            );

            assertFalse(results.isEmpty());
            Object tags = results.get(0).get("tags");
            assertNotNull(tags);
        }

        @Test
        void getColumnValue_regularType_callsSuper() throws SQLException {
            ResultSet rs = mock(ResultSet.class);
            ResultSetMetaData metaData = mock(ResultSetMetaData.class);

            when(rs.getMetaData()).thenReturn(metaData);
            when(metaData.getColumnTypeName(1)).thenReturn("int4");
            when(rs.getObject(1)).thenReturn(42);

            SimpleRowMapper mapper = new SimpleRowMapper(dialect);
            Object result = mapper.getColumnValue(rs, 1);

            assertEquals(42, result);
        }
    }

    @Nested
    @DisplayName("SqlCreatorTemplate Tests")
    class SqlCreatorTemplateTests {

        private SqlCreatorTemplate sqlCreatorTemplate;

        @BeforeEach
        void setup() {
            TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Plain);
            sqlCreatorTemplate = new SqlCreatorTemplate(templateEngine, jdbcManager);
        }

        @Test
        void templateEngine_returnsEngine() {
            assertNotNull(sqlCreatorTemplate.templateEngine());
        }

        @Test
        void jdbcManager_returnsManager() {
            assertEquals(jdbcManager, sqlCreatorTemplate.jdbcManager());
        }
    }
}
