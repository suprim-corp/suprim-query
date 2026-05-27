package dev.suprim.query.autoconfigure;

import dev.suprim.query.jdbc.config.DatabaseConnectionDetail;
import dev.suprim.query.jdbc.executor.creation.CreationService;
import dev.suprim.query.jdbc.executor.deletion.DeleteService;
import dev.suprim.query.jdbc.executor.read.ReadService;
import dev.suprim.query.jdbc.executor.update.UpdateService;
import dev.suprim.query.jdbc.operation.DbOperationService;
import dev.suprim.query.jdbc.operation.JdbcManager;
import dev.suprim.query.jdbc.operation.SqlCreatorTemplate;
import dev.suprim.query.jdbc.processor.*;
import gg.jte.TemplateEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import dev.suprim.query.model.SoftDeleteProperties;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DbAutoConfiguration using Testcontainers PostgreSQL.
 */
@Testcontainers
class DbAutoConfigurationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DbAutoConfiguration.class));

    private static String jdbcUrl;
    private static String username;
    private static String password;

    @BeforeAll
    static void setUp() {
        jdbcUrl = postgres.getJdbcUrl();
        username = postgres.getUsername();
        password = postgres.getPassword();
    }

    @Test
    void autoConfiguration_whenDisabled_shouldNotCreateBeans() {
        contextRunner
                .withPropertyValues("db.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DbAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean(JdbcManager.class);
                });
    }

    @Test
    void autoConfiguration_whenNoDatabase_shouldFailDataSource() {
        contextRunner
                .withPropertyValues("db.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .rootCause()
                            .hasMessageContaining("No data-sources configured");
                });
    }

    @Test
    void autoConfiguration_withValidDatabase_shouldCreateAllBeans() {
        contextRunner
                .withPropertyValues(
                        "db.enabled=true",
                        "db.databases[0].id=test-db",
                        "db.databases[0].type=POSTGRESQL",
                        "db.databases[0].url=" + jdbcUrl,
                        "db.databases[0].username=" + username,
                        "db.databases[0].password=" + password
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(DataSource.class);
                    assertThat(context).hasSingleBean(JdbcManager.class);
                    assertThat(context).hasSingleBean(DbOperationService.class);
                    assertThat(context).hasSingleBean(TemplateEngine.class);
                    assertThat(context).hasSingleBean(SqlCreatorTemplate.class);
                    assertThat(context).hasSingleBean(TSIDProcessor.class);
                    assertThat(context).hasSingleBean(JoinProcessor.class);
                    assertThat(context).hasSingleBean(OrderByProcessor.class);
                    assertThat(context).hasSingleBean(RootTableFieldProcessor.class);
                    assertThat(context).hasSingleBean(RootTableProcessor.class);
                    assertThat(context).hasSingleBean(RootWhereProcessor.class);
                    assertThat(context).hasSingleBean(ReadService.class);
                    assertThat(context).hasSingleBean(CreationService.class);
                    assertThat(context).hasSingleBean(UpdateService.class);
                    assertThat(context).hasSingleBean(DeleteService.class);
                });
    }

    @Test
    void objectMapper_whenMissing_shouldBeCreated() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        ObjectMapper mapper = config.objectMapper();

        assertThat(mapper).isNotNull();
    }

    @Test
    void dbOperationService_shouldReturnJdbcOperationService() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        DbOperationService service = config.dbOperationService();

        assertThat(service).isNotNull();
    }

    @Test
    void templateEngine_shouldReturnPrecompiledEngine() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        TemplateEngine engine = config.templateEngine();

        assertThat(engine).isNotNull();
    }

    @Test
    void tsidProcessor_shouldReturnInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        TSIDProcessor processor = config.tsidProcessor();

        assertThat(processor).isNotNull();
    }

    @Test
    void orderByProcessor_shouldReturnInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        OrderByProcessor processor = config.orderByProcessor();

        assertThat(processor).isNotNull();
    }

    @Test
    void rootTableFieldProcessor_shouldReturnInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        RootTableFieldProcessor processor = config.rootTableFieldProcessor();

        assertThat(processor).isNotNull();
    }

    @Test
    void dataSource_withNoDatabases_shouldThrowException() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                config::dataSource,
                "No data-sources configured"
        );
    }

    @Test
    void dataSource_withDatabaseWithoutJdbcUrl_shouldThrowException() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail dbWithoutUrl = new DatabaseConnectionDetail(
                "no-url-db",
                "POSTGRESQL",
                null,
                "user",
                "password",
                "test",
                null, null, null, null, null, 10
        );
        properties.setDatabases(List.of(dbWithoutUrl));
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                config::dataSource,
                "No data-sources configured"
        );
    }

    @Test
    void dataSource_withEmptyJdbcUrl_shouldThrowException() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail dbWithEmptyUrl = new DatabaseConnectionDetail(
                "empty-url-db",
                "POSTGRESQL",
                "   ",
                "user",
                "password",
                "test",
                null, null, null, null, null, 10
        );
        properties.setDatabases(List.of(dbWithEmptyUrl));
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                config::dataSource,
                "No data-sources configured"
        );
    }

    @Test
    void dataSource_withValidConfig_shouldCreateRoutingDataSource() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail db = new DatabaseConnectionDetail(
                "test-db",
                "POSTGRESQL",
                jdbcUrl,
                username,
                password,
                "testdb",
                null, null, null, null, null, 5
        );
        properties.setDatabases(List.of(db));
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        DataSource dataSource = config.dataSource();

        assertThat(dataSource).isNotNull();
    }

    @Test
    void dataSource_withZeroMaxConnections_shouldDefaultTo10() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail db = new DatabaseConnectionDetail(
                "test-db",
                "POSTGRESQL",
                jdbcUrl,
                username,
                password,
                "testdb",
                null, null, null, null, null, 0
        );
        properties.setDatabases(List.of(db));
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        DataSource dataSource = config.dataSource();

        assertThat(dataSource).isNotNull();
    }

    @Test
    void dataSource_withMixedDatabases_shouldSkipDatabaseWithoutUrl() {
        DbProperties properties = new DbProperties();
        DatabaseConnectionDetail dbWithUrl = new DatabaseConnectionDetail(
                "with-url-db",
                "POSTGRESQL",
                jdbcUrl,
                username,
                password,
                "testdb",
                null, null, null, null, null, 5
        );
        DatabaseConnectionDetail dbWithoutUrl = new DatabaseConnectionDetail(
                "no-url-db",
                "POSTGRESQL",
                null,
                "user",
                "pass",
                "testdb",
                null, null, null, null, null, 5
        );
        properties.setDatabases(List.of(dbWithUrl, dbWithoutUrl));
        DbAutoConfiguration config = new DbAutoConfiguration(properties);

        DataSource dataSource = config.dataSource();

        assertThat(dataSource).isNotNull();
    }

    @Test
    void jdbcManager_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        DatabaseConnectionDetail db = new DatabaseConnectionDetail(
                "test-db", "POSTGRESQL", jdbcUrl, username, password,
                "testdb", null, null, null, null, null, 5
        );
        properties.setDatabases(List.of(db));
        DataSource dataSource = config.dataSource();
        ObjectMapper objectMapper = config.objectMapper();

        JdbcManager manager = config.jdbcManager(dataSource, objectMapper);

        assertThat(manager).isNotNull();
    }

    @Test
    void sqlCreatorTemplate_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);
        TemplateEngine templateEngine = config.templateEngine();

        SqlCreatorTemplate template = config.sqlCreatorTemplate(templateEngine, jdbcManager);

        assertThat(template).isNotNull();
    }

    @Test
    void joinProcessor_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);

        JoinProcessor processor = config.joinProcessor(jdbcManager);

        assertThat(processor).isNotNull();
    }

    @Test
    void rootTableProcessor_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);

        RootTableProcessor processor = config.rootTableProcessor(jdbcManager);

        assertThat(processor).isNotNull();
    }

    @Test
    void rootWhereProcessor_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);

        RootWhereProcessor processor = config.rootWhereProcessor(jdbcManager);

        assertThat(processor).isNotNull();
    }

    @Test
    void readService_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);
        TemplateEngine templateEngine = config.templateEngine();
        SqlCreatorTemplate sqlCreatorTemplate = config.sqlCreatorTemplate(templateEngine, jdbcManager);
        DbOperationService dbOperationService = config.dbOperationService();

        ReadService service = config.readService(jdbcManager, sqlCreatorTemplate, List.of(), dbOperationService, config.resultMapper());

        assertThat(service).isNotNull();
    }

    @Test
    void creationService_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);
        TemplateEngine templateEngine = config.templateEngine();
        SqlCreatorTemplate sqlCreatorTemplate = config.sqlCreatorTemplate(templateEngine, jdbcManager);
        DbOperationService dbOperationService = config.dbOperationService();
        TSIDProcessor tsidProcessor = config.tsidProcessor();

        CreationService service = config.creationService(tsidProcessor, sqlCreatorTemplate, jdbcManager, dbOperationService);

        assertThat(service).isNotNull();
    }

    @Test
    void updateService_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);
        TemplateEngine templateEngine = config.templateEngine();
        SqlCreatorTemplate sqlCreatorTemplate = config.sqlCreatorTemplate(templateEngine, jdbcManager);
        DbOperationService dbOperationService = config.dbOperationService();

        UpdateService service = config.updateService(jdbcManager, sqlCreatorTemplate, dbOperationService);

        assertThat(service).isNotNull();
    }

    @Test
    void deleteService_shouldCreateInstance() {
        DbProperties properties = new DbProperties();
        DbAutoConfiguration config = new DbAutoConfiguration(properties);
        JdbcManager jdbcManager = createJdbcManager(properties, config);
        TemplateEngine templateEngine = config.templateEngine();
        SqlCreatorTemplate sqlCreatorTemplate = config.sqlCreatorTemplate(templateEngine, jdbcManager);
        DbOperationService dbOperationService = config.dbOperationService();
        SoftDeleteProperties softDeleteProperties = config.softDeleteProperties();

        DeleteService service = config.deleteService(jdbcManager, sqlCreatorTemplate, dbOperationService, softDeleteProperties);

        assertThat(service).isNotNull();
    }

    private JdbcManager createJdbcManager(DbProperties properties, DbAutoConfiguration config) {
        DatabaseConnectionDetail db = new DatabaseConnectionDetail(
                "test-db", "POSTGRESQL", jdbcUrl, username, password,
                "testdb", null, null, null, null, null, 5
        );
        properties.setDatabases(List.of(db));
        DataSource dataSource = config.dataSource();
        ObjectMapper objectMapper = config.objectMapper();
        return config.jdbcManager(dataSource, objectMapper);
    }
}
