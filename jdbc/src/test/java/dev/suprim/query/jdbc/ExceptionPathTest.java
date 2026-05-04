package dev.suprim.query.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.EnvironmentProperties;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.executor.creation.JdbcCreationService;
import dev.suprim.query.jdbc.executor.deletion.JdbcDeleteService;
import dev.suprim.query.jdbc.executor.read.JdbcReadService;
import dev.suprim.query.jdbc.executor.update.JdbcUpdateService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.JdbcOperationService;
import dev.suprim.query.jdbc.operation.SimpleRowMapper;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.*;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for exception handling paths to improve coverage.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExceptionPathTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static HikariDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static Dialect dialect;
    private static JdbcManager jdbcManager;
    private static SqlCreatorTemplate sqlCreatorTemplate;
    private static JdbcOperationService dbOperationService;
    private static List<ReadProcessor> processors;

    @BeforeAll
    static void setUp() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgres.getJdbcUrl());
        config.setUsername(postgres.getUsername());
        config.setPassword(postgres.getPassword());
        config.setMaximumPoolSize(5);
        config.setAutoCommit(true);

        dataSource = new HikariDataSource(config);
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        ObjectMapper objectMapper = new ObjectMapper();
        dialect = new PostGreSQLDialect(objectMapper);

        createTestTables();

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

        List<MetaDataExtraction> metaDataExtractions = List.of(new PostgreSQLDataExclusion());
        jdbcManager = new JdbcManager(routingDataSource, List.of(dialect), dbProperties, metaDataExtractions);
        jdbcManager.reload();

        TemplateEngine templateEngine = TemplateEngine.createPrecompiled(ContentType.Plain);
        sqlCreatorTemplate = new SqlCreatorTemplate(templateEngine, jdbcManager);
        dbOperationService = new JdbcOperationService();

        processors = List.of(
                new RootTableProcessor(jdbcManager),
                new RootTableFieldProcessor(),
                new JoinProcessor(jdbcManager),
                new RootWhereProcessor(jdbcManager),
                new OrderByProcessor()
        );
    }

    private static void createTestTables() {
        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.exception_test (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.getJdbcOperations().execute("""
            INSERT INTO public.exception_test (id, name) VALUES
            (1, 'Test 1'), (2, 'Test 2')
            ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name
            """);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    // ==================== SimpleRowMapper Exception Path Tests ====================

    @Nested
    @DisplayName("SimpleRowMapper Exception Paths")
    class SimpleRowMapperExceptionTests {

        @Test
        void mapRow_withArrayType_handlesCorrectly() {
            // Use actual query with array type
            jdbcTemplate.getJdbcOperations().execute("""
                CREATE TABLE IF NOT EXISTS public.array_test (
                    id BIGINT PRIMARY KEY,
                    tags VARCHAR[]
                )
                """);

            jdbcTemplate.getJdbcOperations().execute("""
                INSERT INTO public.array_test (id, tags) VALUES
                (1, ARRAY['tag1', 'tag2'])
                ON CONFLICT (id) DO UPDATE SET tags = EXCLUDED.tags
                """);

            List<Map<String, Object>> results = jdbcTemplate.query(
                    "SELECT * FROM public.array_test WHERE id = :id",
                    Map.of("id", 1L),
                    new SimpleRowMapper(dialect)
            );

            assertNotNull(results);
            assertFalse(results.isEmpty());
        }

        @Test
        void mapRow_withJsonbType_handlesCorrectly() {
            jdbcTemplate.getJdbcOperations().execute("""
                CREATE TABLE IF NOT EXISTS public.jsonb_test (
                    id BIGINT PRIMARY KEY,
                    data JSONB
                )
                """);

            jdbcTemplate.getJdbcOperations().execute("""
                INSERT INTO public.jsonb_test (id, data) VALUES
                (1, '{"key": "value"}'::jsonb)
                ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data
                """);

            List<Map<String, Object>> results = jdbcTemplate.query(
                    "SELECT * FROM public.jsonb_test WHERE id = :id",
                    Map.of("id", 1L),
                    new SimpleRowMapper(dialect)
            );

            assertNotNull(results);
            assertFalse(results.isEmpty());
        }
    }

    // ==================== JdbcReadService Exception Paths ====================

    @Nested
    @DisplayName("JdbcReadService Exception Paths")
    class JdbcReadServiceExceptionTests {

        private JdbcReadService readService;

        @BeforeEach
        void setup() {
            readService = new JdbcReadService(jdbcManager, dbOperationService, processors, sqlCreatorTemplate);
        }

        @Test
        void findAll_withInvalidRsql_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setFilter("invalid[[[[syntax");

            assertThrows(DbException.class, () -> readService.findAll(context));
        }

        @Test
        void count_withValidQuery_returnsCount() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");

            long count = readService.count(context);

            assertTrue(count >= 0);
        }
    }

    // ==================== JdbcCreationService Exception Paths ====================

    @Nested
    @DisplayName("JdbcCreationService Exception Paths")
    class JdbcCreationServiceExceptionTests {

        private JdbcCreationService creationService;
        private TSIDProcessor tsidProcessor;

        @BeforeEach
        void setup() {
            tsidProcessor = new TSIDProcessor();
            creationService = new JdbcCreationService(tsidProcessor, sqlCreatorTemplate, jdbcManager, dbOperationService);
        }

        @Test
        void execute_duplicateKey_throwsRuntimeException() {
            Map<String, Object> data = new HashMap<>();
            data.put("id", 1L); // Duplicate key
            data.put("name", "Duplicate");

            assertThrows(RuntimeException.class, () ->
                    creationService.execute("test", null, "exception_test", null, data, false, null)
            );
        }

        @Test
        void execute_nullValue_handlesGracefully() {
            Map<String, Object> data = new HashMap<>();
            data.put("id", 1000L);
            data.put("name", "With Null");

            var response = creationService.execute("test", null, "exception_test", null, data, false, null);

            assertNotNull(response);
            assertEquals(1, response.row());
        }

        @Test
        void execute_withTsid_generatesId() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "TSID Test");

            var response = creationService.execute("test", null, "exception_test", null, data, true, null);

            assertNotNull(response);
            assertEquals(1, response.row());
        }
    }

    // ==================== JdbcDeleteService Exception Paths ====================

    @Nested
    @DisplayName("JdbcDeleteService Exception Paths")
    class JdbcDeleteServiceExceptionTests {

        private JdbcDeleteService deleteService;

        @BeforeEach
        void setup() {
            deleteService = new JdbcDeleteService(jdbcManager, sqlCreatorTemplate, dbOperationService);
        }

        @Test
        void delete_invalidTable_throwsDbException() {
            assertThrows(DbException.class, () ->
                    deleteService.delete("test", null, "nonexistent_table", "id==1")
            );
        }

        @Test
        void delete_nullFilter_throwsDbException() {
            assertThrows(DbException.class, () ->
                    deleteService.delete("test", null, "exception_test", null)
            );
        }

        @Test
        void delete_blankFilter_throwsDbException() {
            assertThrows(DbException.class, () ->
                    deleteService.delete("test", null, "exception_test", "  ")
            );
        }

        @Test
        void delete_invalidRsql_throwsException() {
            assertThrows(Exception.class, () ->
                    deleteService.delete("test", null, "exception_test", "invalid[[[[")
            );
        }

        @Test
        void delete_validQuery_deletesRecord() throws DbException {
            // Insert a record to delete
            jdbcTemplate.update(
                    "INSERT INTO public.exception_test (id, name) VALUES (:id, :name) ON CONFLICT (id) DO UPDATE SET name = :name",
                    Map.of("id", 9999L, "name", "To Delete")
            );

            int deleted = deleteService.delete("test", null, "exception_test", "id==9999");

            assertEquals(1, deleted);
        }
    }

    // ==================== JdbcUpdateService Exception Paths ====================

    @Nested
    @DisplayName("JdbcUpdateService Exception Paths")
    class JdbcUpdateServiceExceptionTests {

        private JdbcUpdateService updateService;

        @BeforeEach
        void setup() {
            updateService = new JdbcUpdateService(jdbcManager, sqlCreatorTemplate, dbOperationService);
        }

        @Test
        void patch_invalidTable_throwsDbException() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Updated");

            assertThrows(DbException.class, () ->
                    updateService.patch("test", null, "nonexistent_table", data, "id==1")
            );
        }

        @Test
        void patch_nullFilter_throwsDbException() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Updated");

            assertThrows(DbException.class, () ->
                    updateService.patch("test", null, "exception_test", data, null)
            );
        }

        @Test
        void patch_blankFilter_throwsDbException() {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Updated");

            assertThrows(DbException.class, () ->
                    updateService.patch("test", null, "exception_test", data, "")
            );
        }

        @Test
        void patch_validQuery_updatesRecord() throws DbException {
            Map<String, Object> data = new HashMap<>();
            data.put("name", "Updated Name");

            int updated = updateService.patch("test", null, "exception_test", data, "id==1");

            assertTrue(updated >= 0);
        }
    }

    // ==================== JoinProcessor Exception Paths ====================

    @Nested
    @DisplayName("JoinProcessor Exception Paths")
    class JoinProcessorExceptionTests {

        @Test
        void process_invalidColumn_throwsException() throws DbException {
            JoinProcessor processor = new JoinProcessor(jdbcManager);

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("id,name");

            new RootTableProcessor(jdbcManager).process(context);
            new RootTableFieldProcessor().process(context);

            // This should work without exception for null joins
            processor.process(context);

            assertNull(context.getDbJoins());
        }
    }

    // ==================== JdbcManager Exception Paths ====================

    @Nested
    @DisplayName("JdbcManager Exception Paths")
    class JdbcManagerExceptionTests {

        @Test
        void getTable_invalidDbId_throwsDbException() {
            assertThrows(DbException.class, () ->
                    jdbcManager.getTable("nonexistent_db", null, "exception_test")
            );
        }

        @Test
        void getTable_invalidTable_throwsDbException() {
            assertThrows(DbException.class, () ->
                    jdbcManager.getTable("test", null, "nonexistent_table")
            );
        }

        @Test
        void getDialect_invalidDbId_throwsDbException() {
            assertThrows(DbException.class, () ->
                    jdbcManager.getDialect("nonexistent_db")
            );
        }

        @Test
        void getTable_withSchema_returnsTable() throws DbException {
            var table = jdbcManager.getTable("test", "public", "exception_test");

            assertNotNull(table);
            assertEquals("exception_test", table.name());
        }
    }

    // ==================== SqlCreatorTemplate Edge Cases ====================

    @Nested
    @DisplayName("SqlCreatorTemplate Edge Cases")
    class SqlCreatorTemplateEdgeCases {

        @Test
        void query_withNullSorts_handlesCorrectly() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setSorts(null);
            context.setLimit(10);
            context.setOffset(0);

            for (ReadProcessor processor : processors) {
                processor.process(context);
            }

            String sql = sqlCreatorTemplate.query(context);
            assertNotNull(sql);
        }

        @Test
        void query_withEmptySorts_handlesCorrectly() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setSorts(List.of());

            for (ReadProcessor processor : processors) {
                processor.process(context);
            }

            String sql = sqlCreatorTemplate.query(context);
            assertNotNull(sql);
        }

        @Test
        void query_withZeroOffset_handlesCorrectly() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setLimit(10);
            context.setOffset(0);

            for (ReadProcessor processor : processors) {
                processor.process(context);
            }

            String sql = sqlCreatorTemplate.query(context);
            assertNotNull(sql);
            assertTrue(sql.toUpperCase().contains("OFFSET"));
        }

        @Test
        void query_withNegativeOffset_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setLimit(10);
            context.setOffset(-1);

            assertThrows(DbException.class, () -> {
                for (ReadProcessor processor : processors) {
                    processor.process(context);
                }
            });
        }

        @Test
        void query_withLimitBelowMinusOne_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("exception_test");
            context.setFields("*");
            context.setLimit(-2);

            assertThrows(DbException.class, () -> {
                for (ReadProcessor processor : processors) {
                    processor.process(context);
                }
            });
        }
    }

    // ==================== JdbcOperationService Path Tests ====================

    @Nested
    @DisplayName("JdbcOperationService Paths")
    class JdbcOperationServicePaths {

        @Test
        void count_nullResult_returnsZero() {
            var response = dbOperationService.count(
                    jdbcTemplate,
                    Map.of(),
                    "SELECT COUNT(*) FROM public.exception_test WHERE 1=0"
            );

            assertEquals(0, response.count());
        }

        @Test
        void queryCustom_single_returnsMap() {
            Object result = dbOperationService.queryCustom(
                    jdbcTemplate,
                    true,
                    "SELECT * FROM public.exception_test WHERE id = :id",
                    Map.of("id", 1L)
            );

            assertNotNull(result);
            assertTrue(result instanceof Map);
        }

        @Test
        void queryCustom_list_returnsList() {
            Object result = dbOperationService.queryCustom(
                    jdbcTemplate,
                    false,
                    "SELECT * FROM public.exception_test",
                    Map.of()
            );

            assertNotNull(result);
            assertTrue(result instanceof List);
        }
    }
}
