package dev.suprim.query.jdbc.processor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.exception.DbException;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.EnvironmentProperties;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbSort;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.model.JoinDetail;
import dev.suprim.query.model.JoinType;
import dev.suprim.query.model.context.ReadContext;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProcessorTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static HikariDataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static Dialect dialect;
    private static JdbcManager jdbcManager;

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

        // Create tables first
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
    }

    private static void createTestTables() {
        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.users (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255),
                department_id BIGINT
            )
            """);

        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.departments (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL
            )
            """);

        jdbcTemplate.getJdbcOperations().execute("""
            CREATE TABLE IF NOT EXISTS public.orders (
                id BIGINT PRIMARY KEY,
                user_id BIGINT,
                product_name VARCHAR(255),
                total NUMERIC(10,2)
            )
            """);
    }

    @AfterAll
    static void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Nested
    @DisplayName("TSIDProcessor Tests")
    class TSIDProcessorTests {

        private TSIDProcessor tsidProcessor;

        @BeforeEach
        void setup() {
            tsidProcessor = new TSIDProcessor();
        }

        @Test
        void processTsId_intFamilyPk_generatesLongTsid() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(true);
            when(pkColumn.name()).thenReturn("id");
            when(pkColumn.columnDataTypeName()).thenReturn("int8");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertNotNull(keys.get("id"));
            assertTrue(keys.get("id") instanceof Long);
            assertEquals(keys.get("id"), data.get("id"));
        }

        @Test
        void processTsId_stringFamilyPk_generatesStringTsid() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(false);
            when(pkColumn.isStringFamily()).thenReturn(true);
            when(pkColumn.name()).thenReturn("id");
            when(pkColumn.columnDataTypeName()).thenReturn("varchar");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertNotNull(keys.get("id"));
            assertTrue(keys.get("id") instanceof String);
            assertEquals(keys.get("id"), data.get("id"));
        }

        @Test
        void processTsId_generatedPk_skipsTsidGeneration() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(true);
            when(pkColumn.autoIncremented()).thenReturn(false);

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertTrue(keys.isEmpty());
        }

        @Test
        void processTsId_autoIncrementedPk_skipsTsidGeneration() throws DbException {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(true);

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pkColumn));

            assertTrue(keys.isEmpty());
        }

        @Test
        void processTsId_unknownTypeFamily_throwsDbException() {
            DbColumn pkColumn = mock(DbColumn.class);
            when(pkColumn.generated()).thenReturn(false);
            when(pkColumn.autoIncremented()).thenReturn(false);
            when(pkColumn.isIntFamily()).thenReturn(false);
            when(pkColumn.isStringFamily()).thenReturn(false);
            when(pkColumn.columnDataTypeName()).thenReturn("unknown");

            Map<String, Object> data = new HashMap<>();

            DbException ex = assertThrows(DbException.class,
                    () -> tsidProcessor.processTsId(data, List.of(pkColumn)));

            assertTrue(ex.getMessage().contains("Unable to detect data type family"));
        }

        @Test
        void processTsId_multiplePks_generatesMultipleTsids() throws DbException {
            DbColumn pk1 = mock(DbColumn.class);
            when(pk1.generated()).thenReturn(false);
            when(pk1.autoIncremented()).thenReturn(false);
            when(pk1.isIntFamily()).thenReturn(true);
            when(pk1.name()).thenReturn("id1");
            when(pk1.columnDataTypeName()).thenReturn("int8");

            DbColumn pk2 = mock(DbColumn.class);
            when(pk2.generated()).thenReturn(false);
            when(pk2.autoIncremented()).thenReturn(false);
            when(pk2.isIntFamily()).thenReturn(false);
            when(pk2.isStringFamily()).thenReturn(true);
            when(pk2.name()).thenReturn("id2");
            when(pk2.columnDataTypeName()).thenReturn("varchar");

            Map<String, Object> data = new HashMap<>();

            Map<String, Object> keys = tsidProcessor.processTsId(data, List.of(pk1, pk2));

            assertEquals(2, keys.size());
            assertTrue(keys.get("id1") instanceof Long);
            assertTrue(keys.get("id2") instanceof String);
        }
    }

    @Nested
    @DisplayName("RootTableProcessor Tests")
    class RootTableProcessorTests {

        private RootTableProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootTableProcessor(jdbcManager);
        }

        @Test
        void process_validTable_setsRootInContext() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");

            processor.process(context);

            assertNotNull(context.getRoot());
            assertEquals("users", context.getRoot().name());
        }

        @Test
        void process_withSchema_setsRootInContext() throws DbException {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setSchemaName("public");
            context.setTableName("departments");

            processor.process(context);

            assertNotNull(context.getRoot());
            assertEquals("departments", context.getRoot().name());
        }

        @Test
        void process_invalidTable_throwsDbException() {
            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("nonexistent");

            assertThrows(DbException.class, () -> processor.process(context));
        }
    }

    @Nested
    @DisplayName("RootTableFieldProcessor Tests")
    class RootTableFieldProcessorTests {

        private RootTableFieldProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootTableFieldProcessor();
        }

        @Test
        void process_nullFields_doesNotSetColumns() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setRoot(table);
            context.setFields(null);

            processor.process(context);

            assertNull(context.getCols());
        }

        @Test
        void process_allFields_includesAllColumns() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setRoot(table);
            context.setFields("*");

            processor.process(context);

            assertNotNull(context.getCols());
            assertFalse(context.getCols().isEmpty());
        }

        @Test
        void process_specificFields_includesOnlySpecified() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setRoot(table);
            context.setFields("id,name");

            processor.process(context);

            assertNotNull(context.getCols());
            assertEquals(2, context.getCols().size());
        }

        @Test
        void process_blankFields_throwsException() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setRoot(table);
            context.setFields("   ");

            // Blank fields get trimmed but not caught by null check,
            // causing an exception when trying to parse empty column name
            assertThrows(RuntimeException.class, () -> processor.process(context));
        }
    }

    @Nested
    @DisplayName("OrderByProcessor Tests")
    class OrderByProcessorTests {

        private OrderByProcessor processor;

        @BeforeEach
        void setup() {
            processor = new OrderByProcessor();
        }

        @Test
        void process_nullSorts_doesNothing() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(null);

            processor.process(context);

            assertNull(context.getDbSortList());
        }

        @Test
        void process_emptySorts_doesNothing() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(List.of());

            processor.process(context);

            assertNull(context.getDbSortList());
        }

        @Test
        void process_withSort_addsSortToContext() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(List.of("name"));

            processor.process(context);

            assertNotNull(context.getDbSortList());
            assertEquals(1, context.getDbSortList().size());
            assertEquals("ASC", context.getDbSortList().get(0).sortDirection());
        }

        @Test
        void process_withAscSort_setsAscDirection() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(List.of("name;ASC"));

            processor.process(context);

            assertNotNull(context.getDbSortList());
            assertEquals("ASC", context.getDbSortList().get(0).sortDirection());
        }

        @Test
        void process_withDescSort_setsDescDirection() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(List.of("name;DESC"));

            processor.process(context);

            assertNotNull(context.getDbSortList());
            assertEquals("DESC", context.getDbSortList().get(0).sortDirection());
        }

        @Test
        void process_multipleSorts_addsAllSorts() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setTableName("users");
            context.setRoot(table);
            context.setSorts(List.of("name;ASC", "id;DESC"));

            processor.process(context);

            assertNotNull(context.getDbSortList());
            assertEquals(2, context.getDbSortList().size());
        }
    }

    @Nested
    @DisplayName("RootWhereProcessor Tests")
    class RootWhereProcessorTests {

        private RootWhereProcessor processor;

        @BeforeEach
        void setup() {
            processor = new RootWhereProcessor(jdbcManager);
        }

        @Test
        void process_nullFilter_doesNotSetWhere() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(table);
            context.setCols(table.buildColumns());
            context.setFilter(null);

            processor.process(context);

            assertNull(context.getRootWhere());
        }

        @Test
        void process_blankFilter_doesNotSetWhere() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(table);
            context.setCols(table.buildColumns());
            context.setFilter("   ");

            processor.process(context);

            assertNull(context.getRootWhere());
        }

        @Test
        void process_withSimpleFilter_setsWhere() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(table);
            context.setCols(table.buildColumns());
            context.setFilter("id==1");

            processor.process(context);

            assertNotNull(context.getRootWhere());
            assertNotNull(context.getParamMap());
        }

        @Test
        void process_invalidRsql_throwsDbException() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setTableName("users");
            context.setRoot(table);
            context.setCols(table.buildColumns());
            context.setFilter("invalid rsql ;;; [[[");

            assertThrows(DbException.class, () -> processor.process(context));
        }
    }

    @Nested
    @DisplayName("JoinProcessor Tests")
    class JoinProcessorTests {

        private JoinProcessor processor;

        @BeforeEach
        void setup() {
            processor = new JoinProcessor(jdbcManager);
        }

        @Test
        void process_nullJoins_doesNothing() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(null);

            processor.process(context);

            // When no joins processed, dbJoins stays null or empty
            assertTrue(context.getDbJoins() == null || context.getDbJoins().isEmpty());
        }

        @Test
        void process_emptyJoins_doesNothing() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of());

            processor.process(context);

            // When no joins processed, dbJoins stays null or empty
            assertTrue(context.getDbJoins() == null || context.getDbJoins().isEmpty());
        }

        @Test
        void process_withJoin_addsJoinToContext() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            // JoinDetail(schemaName, table, withTable, fields, on, filter, joinType)
            JoinDetail joinDetail = new JoinDetail(
                    null,           // schemaName
                    "departments",  // table
                    null,           // withTable
                    null,           // fields
                    List.of("department_id==id"),  // on
                    null,           // filter
                    JoinType.LEFT   // joinType
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of(joinDetail));
            context.setCols(table.buildColumns());

            processor.process(context);

            assertFalse(context.getDbJoins().isEmpty());
            assertEquals(1, context.getDbJoins().size());
        }

        @Test
        void process_withMultipleJoins_addsAllJoins() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            JoinDetail join1 = new JoinDetail(
                    null,           // schemaName
                    "departments",  // table
                    null,           // withTable
                    null,           // fields
                    List.of("department_id==id"),  // on
                    null,           // filter
                    JoinType.LEFT   // joinType
            );

            JoinDetail join2 = new JoinDetail(
                    null,           // schemaName
                    "orders",       // table
                    "users",        // withTable
                    null,           // fields
                    List.of("id==user_id"),  // on
                    null,           // filter
                    JoinType.LEFT   // joinType
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of(join1, join2));
            context.setCols(table.buildColumns());

            processor.process(context);

            assertEquals(2, context.getDbJoins().size());
        }

        @Test
        void process_withJoinFilter_parsesRsqlFilter() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            JoinDetail joinDetail = new JoinDetail(
                    null,           // schemaName
                    "orders",       // table
                    null,           // withTable
                    null,           // fields
                    List.of("id==user_id"),  // on
                    "total=gt=100", // filter
                    JoinType.LEFT   // joinType
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of(joinDetail));
            context.setCols(table.buildColumns());

            processor.process(context);

            assertFalse(context.getDbJoins().isEmpty());
        }

        @Test
        void process_nullFields_includesAllColumns() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            JoinDetail joinDetail = new JoinDetail(
                    null,           // schemaName
                    "departments",  // table
                    null,           // withTable
                    null,           // fields (null = all)
                    List.of("department_id==id"),  // on
                    null,           // filter
                    JoinType.LEFT   // joinType
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of(joinDetail));
            context.setCols(table.buildColumns());

            int initialColumns = context.getCols().size();

            processor.process(context);

            // Columns from joined table should be added
            assertTrue(context.getCols().size() > initialColumns);
        }

        @Test
        void process_specificFields_includesOnlySpecified() throws DbException {
            DbTable table = jdbcManager.getTable("test", null, "users");

            JoinDetail joinDetail = new JoinDetail(
                    null,           // schemaName
                    "departments",  // table
                    null,           // withTable
                    List.of("name"), // fields (specific)
                    List.of("department_id==id"),  // on
                    null,           // filter
                    JoinType.LEFT   // joinType
            );

            ReadContext context = new ReadContext();
            context.setDbId("test");
            context.setRoot(table);
            context.setJoins(List.of(joinDetail));
            context.setCols(table.buildColumns());

            int initialColumns = context.getCols().size();

            processor.process(context);

            // Only 1 column (name) from joined table should be added
            assertEquals(initialColumns + 1, context.getCols().size());
        }
    }
}
