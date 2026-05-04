package dev.suprim.query.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.suprim.query.dialect.Dialect;
import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.config.DatabaseProperties;
import dev.suprim.query.jdbc.config.EnvironmentProperties;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.JdbcOperationService;
import dev.suprim.query.jdbc.operation.SimpleRowMapper;
import dev.suprim.query.model.DbColumn;
import dev.suprim.query.model.DbTable;
import dev.suprim.query.postgresql.PostGreSQLDialect;
import dev.suprim.query.postgresql.PostgreSQLDataExclusion;
import dev.suprim.query.support.MetaDataExtraction;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JdbcOperationsIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate jdbcTemplate;
    private static JdbcOperationService operationService;
    private static JdbcManager jdbcManager;
    private static Dialect dialect;

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

        List<MetaDataExtraction> metaDataExtractions = List.of(new PostgreSQLDataExclusion());
        jdbcManager = new JdbcManager(dataSource, List.of(dialect), dbProperties, metaDataExtractions);
        operationService = new JdbcOperationService();

        createTestTable();
    }

    private static void createTestTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS public.users (
                id BIGINT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) UNIQUE,
                age INT,
                metadata JSONB,
                tags VARCHAR[] DEFAULT '{}',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        jdbcTemplate.getJdbcOperations().execute(sql);
    }

    @Test
    @Order(1)
    void testInsert() {
        String sql = "INSERT INTO public.users (id, name, email, age) VALUES (:id, :name, :email, :age)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", 1L)
                .addValue("name", "John Doe")
                .addValue("email", "john@example.com")
                .addValue("age", 30);

        int rows = jdbcTemplate.update(sql, params);
        assertEquals(1, rows);
    }

    @Test
    @Order(2)
    void testSelect() {
        String sql = "SELECT id, name, email, age FROM public.users WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", 1L);

        List<Map<String, Object>> results = jdbcTemplate.query(sql, params, new SimpleRowMapper(dialect));

        assertFalse(results.isEmpty());
        assertEquals("John Doe", results.get(0).get("name"));
        assertEquals("john@example.com", results.get(0).get("email"));
    }

    @Test
    @Order(3)
    void testUpdate() {
        String sql = "UPDATE public.users SET age = :age WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", 1L)
                .addValue("age", 31);

        int rows = jdbcTemplate.update(sql, params);
        assertEquals(1, rows);

        String selectSql = "SELECT age FROM public.users WHERE id = :id";
        Integer age = jdbcTemplate.queryForObject(selectSql, params, Integer.class);
        assertEquals(31, age);
    }

    @Test
    @Order(4)
    void testJsonbInsertAndSelect() {
        String sql = "INSERT INTO public.users (id, name, email, metadata) VALUES (:id, :name, :email, :metadata::jsonb)";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", 2L)
                .addValue("name", "Jane Doe")
                .addValue("email", "jane@example.com")
                .addValue("metadata", "{\"role\": \"admin\", \"permissions\": [\"read\", \"write\"]}");

        int rows = jdbcTemplate.update(sql, params);
        assertEquals(1, rows);

        String selectSql = "SELECT metadata FROM public.users WHERE id = :id";
        List<Map<String, Object>> results = jdbcTemplate.query(selectSql, params, new SimpleRowMapper(dialect));

        assertFalse(results.isEmpty());
        assertNotNull(results.get(0).get("metadata"));
    }

    @Test
    @Order(5)
    void testCount() {
        String sql = "SELECT COUNT(*) FROM public.users";
        Long count = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
        assertTrue(count >= 2);
    }

    @Test
    @Order(6)
    void testDelete() {
        String sql = "DELETE FROM public.users WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", 2L);

        int rows = jdbcTemplate.update(sql, params);
        assertEquals(1, rows);
    }

    @Test
    @Order(7)
    void testDialectRenderTableName() {
        DbTable table = new DbTable(
                "public", "users", "public.users", "u",
                List.of(new DbColumn("users", "id", "", "u", true, "int8", false, false, Long.class, "\"", "")),
                "TABLE", "\""
        );

        String rendered = dialect.renderTableName(table, false, false);
        assertEquals("\"public\".\"users\" u", rendered);

        String withoutAlias = dialect.renderTableNameWithoutAlias(table);
        assertEquals("\"public\".\"users\"", withoutAlias);
    }

    @Test
    @Order(8)
    void testDialectIsSupportedDb() {
        assertTrue(dialect.isSupportedDb("PostgreSQL", 16));
        assertFalse(dialect.isSupportedDb("MySQL", 8));
    }

    @AfterAll
    static void tearDown() {
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.close();
        }
    }
}
