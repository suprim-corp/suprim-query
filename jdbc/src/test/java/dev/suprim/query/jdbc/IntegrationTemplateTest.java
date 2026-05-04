package dev.suprim.query.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.EnvironmentProperties;
import dev.suprim.query.jdbc.config.RoutingDataSource;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.support.JdbcMetaDataProvider;
import dev.suprim.query.model.DbMeta;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IntegrationTemplateTest {

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
            CREATE TABLE IF NOT EXISTS public.test_metadata (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL
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
    @DisplayName("JdbcMetaDataProvider Tests")
    class JdbcMetaDataProviderTests {

        @Test
        void processMetaData_withIncludedSchemas_extractsTables() throws Exception {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));
            provider.addExtraction(new PostgreSQLDataExclusion());

            DbMeta result = JdbcUtils.extractDatabaseMetaData(dataSource, provider);

            assertNotNull(result);
            assertNotNull(result.productName());
            assertTrue(result.productName().contains("PostgreSQL"));
            assertNotNull(result.dbTables());
            assertFalse(result.dbTables().isEmpty());
        }

        @Test
        void processMetaData_withAllSchemas_extractsAllTables() throws Exception {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(true, List.of());
            provider.addExtraction(new PostgreSQLDataExclusion());

            DbMeta result = JdbcUtils.extractDatabaseMetaData(dataSource, provider);

            assertNotNull(result);
            assertFalse(result.dbTables().isEmpty());
        }

        @Test
        void processMetaData_noExtraction_throwsRuntimeException() {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));

            assertThrows(RuntimeException.class, () ->
                    JdbcUtils.extractDatabaseMetaData(dataSource, provider)
            );
        }

        @Test
        void addExtraction_addsToList() throws Exception {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));
            PostgreSQLDataExclusion extraction = new PostgreSQLDataExclusion();

            provider.addExtraction(extraction);

            DbMeta result = JdbcUtils.extractDatabaseMetaData(dataSource, provider);
            assertNotNull(result);
        }

        @Test
        void processMetaData_extractsVersionInfo() throws Exception {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));
            provider.addExtraction(new PostgreSQLDataExclusion());

            DbMeta result = JdbcUtils.extractDatabaseMetaData(dataSource, provider);

            assertNotNull(result.driverName());
            assertNotNull(result.driverVersion());
            assertTrue(result.majorVersion() > 0);
        }

        @Test
        void processMetaData_findsTestMetadataTable() throws Exception {
            JdbcMetaDataProvider provider = new JdbcMetaDataProvider(false, List.of("public"));
            provider.addExtraction(new PostgreSQLDataExclusion());

            DbMeta result = JdbcUtils.extractDatabaseMetaData(dataSource, provider);

            boolean foundTestTable = result.dbTables().stream()
                    .anyMatch(t -> "test_metadata".equals(t.name()));
            assertTrue(foundTestTable, "Should find test_metadata table");
        }
    }

    @Nested
    @DisplayName("JdbcManager Integration Tests")
    class JdbcManagerIntegrationTests {

        @Test
        void reload_loadsAllTables() throws Exception {
            DbTable table = jdbcManager.getTable("test", null, "test_metadata");
            assertNotNull(table, "JdbcManager should load test_metadata table");
        }

        @Test
        void getTable_existingTable_returnsTable() throws Exception {
            DbTable table = jdbcManager.getTable("test", null, "test_metadata");

            assertNotNull(table);
            assertEquals("test_metadata", table.name());
        }

        @Test
        void getDialect_returnsCorrectDialect() throws Exception {
            Dialect dialect = jdbcManager.getDialect("test");

            assertNotNull(dialect);
            assertTrue(dialect.isSupportedDb("PostgreSQL", 16));
        }

        @Test
        void getNamedParameterJdbcTemplate_returnsTemplate() {
            NamedParameterJdbcTemplate template = jdbcManager.getNamedParameterJdbcTemplate("test");

            assertNotNull(template);
        }

        @Test
        void getTxnTemplate_returnsTransactionTemplate() {
            var txnTemplate = jdbcManager.getTxnTemplate("test");

            assertNotNull(txnTemplate);
        }
    }

    @Nested
    @DisplayName("Multi-Database Routing Tests")
    class MultiDatabaseRoutingTests {

        @Test
        void routingDataSource_routesToCorrectDatabase() throws Exception {
            DbTable table = jdbcManager.getTable("test", null, "test_metadata");
            assertNotNull(table);
        }

        @Test
        void databaseContextHolder_setAndClear() {
            dev.suprim.query.jdbc.config.DatabaseContextHolder.setCurrentDbId("test");
            assertEquals("test", dev.suprim.query.jdbc.config.DatabaseContextHolder.getCurrentDbId());

            dev.suprim.query.jdbc.config.DatabaseContextHolder.clear();
            assertNull(dev.suprim.query.jdbc.config.DatabaseContextHolder.getCurrentDbId());
        }
    }
}
